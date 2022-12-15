﻿#include "ServerLayer.h"

#include "Prot.h"
#include "server_constants.h"
#include "net/Multiplayer.h"
#include "world/nd_registry.h"
#include "world/World.h"
#include "world/entity/EntityPlayer.h"
#include "glm/gtx/string_cast.hpp"


using namespace nd::net;

bool ServerLayer::PlayerInfo::isAlive() const
{
	return nd::nowTime() - lastSeen < server_const::PLAYER_SERVER_TIMEOUT;
}

void ServerLayer::PlayerInfo::updateLife()
{
	lastSeen = nd::nowTime();
}

ServerLayer::PlayerInfo::PlayerInfo()
{
	updateLife();
}


void ServerLayer::ChunkMail::fillWithChunk(Chunk* chunk)
{
	c_id = chunk->chunkID();

	BlockStruct* b = &chunk->block(0, 0);
	buffer.resize(sizeof(BlockStruct) * WORLD_CHUNK_AREA);
	memcpy(buffer.data(), b, buffer.size());

	pending_pieces.clear();
	for (char i = 0; i < CHUNK_PIECE_COUNT; ++i)
		pending_pieces.push_back(i);
}

void ServerLayer::openSocket()
{
	CreateSocketInfo info;
	info.async = true;
	info.port = m_port;
	createSocket(m_socket, info);
}

void ServerLayer::closeSocket()
{
	nd::net::closeSocket(m_socket);
}

ServerLayer::SessionID ServerLayer::allocateNewSessionID()
{
	return ++m_last_id;
}

ServerLayer::ServerLayer(const std::string& name, int port,
                         nd::net::Address lobbyAddress): m_lobby_address(lobbyAddress), m_port(port)
{
	m_name = name;
	m_big_udp_message.buffer.reserve(1500);
}

void ServerLayer::onAttach()
{
	m_death_counter = -1;
	m_should_restart = false;
	ND_TRACE("Starting server on port {}", m_port);
	openSocket();

	WorldInfo info = {};
	strcpy(info.name, m_name.c_str());
	info.chunk_height = 3;
	info.chunk_width = 3;
	info.terrain_level = 16;
	info.time = 0;
	m_world = new World(m_name, info);
	ND_INFO("Generating world {}", info.name);
	if (!m_world->loadWorld())
	{
		ND_INFO("World file does not exist, generating new");
		m_world->genWorld();
	}

	ND_INFO("World is ready :)");
}

void ServerLayer::onDetach()
{
	closeSocket();
	m_world->saveWorld();
	delete m_world;
}

bool ServerLayer::isValidSession(SessionID id)
{
	return m_player_book.contains(id) && m_player_book[id].isValid;
}

// ================ON UPDATE========================
void ServerLayer::onUpdate()
{
	m_log.reserve(5000);
	m_tick++;
	m_log += "\n|=";
	m_log += std::to_string(m_tick);
	m_log += "=|\n";


	/*for (auto& info : m_player_book)
	{
		if (!info.second.isValid)
			continue;
		auto player = m_world->getPlayer(info.first);
		if (player)
		{
			m_log += "Prepos:";
			m_log += glm::to_string(player->getPosition());
			m_log += "|Inputs|";
			for (auto& in : info.second.moves.inputs)
				m_log += in.toString() + ",";
			m_log += "|";
		}
	}*/
	updateDeathCounter();

	if (!m_is_online)
		updateCryForAttention();

	Message& m = m_big_udp_message;
	updateCheckTimeout(m);

	//prepare new space for possible move events from players 
	prepareMove();

	// poll udp
	updateUDP(m);
	// poll tcp
	updateTCP(m);
	// send chunks to clients that requested it
	updateSendChunks();

	//move players
	applyMoves();

	/*for (auto& info : m_player_book)
	{
		if (!info.second.isValid)
			continue;
		auto player = m_world->getPlayer(info.first);
		if (player)
		{
			m_log += "Afterpos:";
			m_log += glm::to_string(player->getPosition());
		}
	}*/

	//send moves back to players
	sendMoves(m);

	//if(m_log.size()>4500)
	//{
	//	ND_INFO("DUMP:\n{}", m_log);
	m_log.clear();
	//}
}

void ServerLayer::updateUDP(nd::net::Message& m)
{
	// udp poll
	while (receive(m_socket, m) == NetResponseFlags_Success)
	{
		// check if message is tcp
		auto sessionID = TCPTunnel::getClientID(m);
		if (sessionID != -1)
		{
			auto it = m_tunnels.find(sessionID);
			if (it == m_tunnels.end())
				continue;
			TCPTunnel& tunnel = it->second;
			tunnel.receiveTunnelUDP(m);
		}

		// normal udp
		ProtocolHeader h;
		if (!NetReader(m.buffer).get(h))
			continue;

		switch (h.action)
		{
		case Prot::InvitationREQ:
			onInvReq(m);
			break;
		case Prot::InvitationACK:
			onInvACK(m);
			break;
		case Prot::ChunkREQ:
			onChunkReq(m);
			break;
		case Prot::PlayerMoves:
			onMove(m);
			break;
		case Prot::Ping:
			h.action = Prot::Pong;
			NetWriter(m.buffer).put(h);
			nd::net::send(m_socket, m);
			break;
		case Prot::ClusterPing:
			if (!m_is_online)
			{
				ND_INFO("Server {} connected to lobby :D", m_socket.m_address.port());
			}
			m_is_online = true;
			sendClusterPong(m.address);
			break;
		case Prot::Quit:
			onQuit(m);
			break;
		default: ;
		}
	}
}

void ServerLayer::updateTCP(nd::net::Message& m)
{
	//tcp poll
	for (auto& [session, tun] : m_tunnels)
	{
		while (tun.read(m))
		{
			ProtocolHeader h;
			if (!NetReader(m.buffer).get(h))
				continue;

			switch (h.action)
			{
			case Prot::Command:
				onCommand(m);
				break;
			case Prot::BlockModify:
				onBlockModify(m, session);
				break;
			}
		}
	}
	flushTCP();
}

void ServerLayer::updateCryForAttention()
{
	if (m_ticks_until_cry-- != 0)
		return;
	m_ticks_until_cry = server_const::CRY_FOR_ATTENTION_INTERVAL;

	sendClusterPong(m_lobby_address);
}

void ServerLayer::updateCheckTimeout(nd::net::Message& m)
{
	std::vector<SessionID> toRemove;
	//throw away dead players
	auto iter = m_player_book.begin();
	while (iter != m_player_book.end())
	{
		if (!iter->second.isAlive())
			toRemove.push_back(iter->first);
		++iter;
	}
	for (auto deadId : toRemove)
		disconnectClient(m, deadId, MES_SERVER_KICK_TIMEOUT);

	// do not throw away dead servers though
}

void ServerLayer::updateSendChunks()
{
	constexpr int CHUNK_PIECES_PER_TICK = 40;
	Message mes;
	mes.buffer.reserve(CHUNK_PIECE_SIZE + 50);

	for (auto& [session, m] : m_chunk_mail)
	{
		for (int i = 0; i < CHUNK_PIECES_PER_TICK; ++i)
		{
			if (!m.pending())
				break;

			auto piece = m.popPendingPiece();
			mes.address = m_player_book[session].address;

			auto ptr = m.pieceMemory(piece);
			auto len = m.pieceLength(piece);

			ChunkProtocol h;
			h.action = Prot::ChunkACK;
			h.c_x = half_int::X(m.c_id);
			h.c_y = half_int::Y(m.c_id);
			h.piece = piece;
			h.session_id = session;

			auto writer = NetWriter(mes.buffer);
			writer.put(h);
			writer.write(ptr, len);

			//ND_TRACE("Sending ChunkPiece ({},{}):{} to {} with length={}", half_int::X(m.c_id), half_int::Y(m.c_id),
			//         (int)piece, m_player_book[session].name, len);
			mes.address = m_player_book[session].address;
			send(m_socket, mes);
		}
	}
}

void ServerLayer::updateDeathCounter()
{
	if (m_death_counter >= 0)
	{
		ND_TRACE("Server stopping in {}", m_death_counter);
		m_death_counter--;
		if (m_death_counter == 0)
		{
			ND_INFO("Disconnecting all players");
			disconnectAll(m_big_udp_message, m_should_exit ? MES_SERVER_KICK_QUIT : MES_SERVER_KICK_RESTART);
		}
	}
}

// ================ON MESSAGE RECEIVE===============

void ServerLayer::onInvReq(nd::net::Message& m)
{
	auto reader = NetReader(m.buffer);

	EstablishConnectionProtocol h;
	if (!reader.get(h))
	{
		ND_WARN("InvREQ: Could not parse");
		return;
	}

	if (m_lobby_address != m.address)
	{
		ND_WARN("InvREQ: Received Inv REQ from someone other than lobby, throwing away. Sus!");
		return;
	}

	ND_INFO("InvREQ: Invitation Request Received from {}", h.player);

	if (m_session_ids.contains(h.player) && m_player_book[m_session_ids[h.player]].isValid)
	{
		ND_WARN("InvREQ: Player {} already exists on the server, you shall not pass!", h.player);
		return; //already here with same name
	}

	SessionID invitationId = m_session_ids.contains(h.player) ? m_session_ids[h.player] : allocateNewSessionID();

	m_session_ids[h.player] = invitationId;
	auto& info = m_player_book[invitationId];
	info.session_id = invitationId;
	info.isValid = false;
	info.name = h.player;

	h.action = Prot::Invitation;
	h.server = m_name;
	h.session_id = info.session_id;

	NetWriter(m.buffer).put(h);
	send(m_socket, m);
	ND_TRACE("InvREQ: Success for {}, sending back invitation with invitation id {} to lobby", h.player, invitationId);
}

#define CHECK_SESSION_VALID()\
	if (!isValidSession(h.session_id))\
	{\
		ND_WARN("Imposter with invalid session id {} from address {}", h.session_id, m.address.toString());\
		return;\
	}

void ServerLayer::onInvACK(nd::net::Message& m)
{
	EstablishConnectionProtocol h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("InvACK: Could not parse");
		return;
	}

	// invitation does not exist
	if (!m_session_ids.contains(h.player))
	{
		ND_WARN("InvACK: Could not acknowledge player {}, invitation {} does not exist", h.player, h.session_id);
		return;
	}

	auto& info = m_player_book[m_session_ids[h.player]];

	// invitation id does not match
	if (h.session_id != info.session_id)
	{
		ND_WARN("InvACK: session ids for player {} do not match (server:{},client:{})", h.player, info.session_id,
		        h.session_id);
		return;
	}

	if (!info.isValid)
	{
		//create tcp tunnel
		m_tunnels.try_emplace(info.session_id, m_socket, m.address, info.session_id, 5000);
		ND_TRACE("Opening TCP tunnel to the player {}", h.player);
		m_world->createPlayer(info.session_id);
	}

	// mark player state (re)verified
	info.address = m.address;
	info.isValid = true;
	info.updateLife();
	ND_INFO("InvACK: Client: {} (re)obtained session ID: {}", m.address.toString(), info.session_id);

	h.action = Prot::SessionCreated;
	h.session_id = info.session_id;

	NetWriter(m.buffer).put(h);
	send(m_socket, m);
	ND_TRACE("InvACK: Sending SessionCreated to {} on {}", h.player, m.address.toString());

	sendCommand(m, info.session_id, "server", "Hello " + info.name + "! Welcome to the server " + m_name);
	broadcastCommand(m, "server", "A wild " + info.name + " has appeared!", h.session_id);
}

void ServerLayer::onChunkReq(nd::net::Message& m)
{
	ChunkProtocol h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("ChunkReq: Could not parse");
		return;
	}
	CHECK_SESSION_VALID();

	ND_TRACE("ChunkReq: Received request for ({},{}):{}, session={}", h.c_x, h.c_y, h.piece, h.session_id);

	auto chunkPtr = m_world->getChunkM(h.c_x, h.c_y);
	if (!chunkPtr)
	{
		ND_WARN("ChunkReq: Invalid chunk");
		return;
	}

	auto& mail = m_chunk_mail[h.session_id];


	if (mail.c_id != chunkPtr->chunkID())
	{
		mail.fillWithChunk(chunkPtr);
	}
	else if (h.piece >= 0 && h.piece < CHUNK_PIECE_COUNT)
	{
		ND_TRACE("ChunkReq: Received request for one piece of chunk({},{}):{}, session={}", h.c_x, h.c_y, h.piece,
		         h.session_id);
		mail.pushPendingPiece(h.piece);
	}
}


void ServerLayer::onCommand(nd::net::Message& m)
{
	CommandProtocol h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("Command: Could not parse");
		return;
	}
	CHECK_SESSION_VALID();

	ND_INFO("Received command: {} from player {}", h.message, m_player_book[h.session_id].name);

	if (nd::SUtil::startsWith(h.message, '/'))
	{
		auto cmd = h.message.substr(1);
		if (cmd == "exit")
		{
			ND_INFO("Command: Received exit");
			scheduleStop(true);
			broadcastCommand(m, "server", "Server exiting, bye bye");
		}
		else if (cmd == "restart")
		{
			ND_INFO("Command: Received restart");
			scheduleStop(false);
			broadcastCommand(m, "server", "Server restarting, bye bye");
		}
		else if (cmd == "fly")
		{
			m_world->getPlayer(h.session_id)->setFly(!m_world->getPlayer(h.session_id)->isFlying());
			ND_INFO("Command: Toggled fly for {} to {}", m_player_book[h.session_id].name,
			        m_world->getPlayer(h.session_id)->isFlying());
		}
		else if (nd::SUtil::startsWith(cmd, "echo "))
		{
			std::string e = cmd.substr(strlen("echo "));
			ND_INFO("Command: Received echo: "+e);
			sendCommand(m, h.session_id, "server", e);
		}
		else
		{
			ND_INFO("Command: Received invalid");
			sendCommand(m, h.session_id, "server", "Invalid command!");
		}
	}
	else
	{
		broadcastCommand(m, m_player_book[h.session_id].name, h.message, h.session_id);
	}
}

void ServerLayer::onQuit(nd::net::Message& m)
{
	ControlProtocol h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("Quit: Could not parse");
		return;
	}
	CHECK_SESSION_VALID();

	ND_INFO("Player {} has disconnected from server", m_player_book[h.session_id].name);
	disconnectClient(m, h.session_id, "", false);
}

void ServerLayer::onBlockModify(nd::net::Message& m, SessionID session)
{
	BlockModify h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("BlockModify: Could not parse");
		return;
	}
	auto player = m_world->getPlayer(session);
	bool cont = ndPhys::contains(player->getCollisionBox(),
	                             glm::vec2(h.x - player->getPosition().x, h.y - player->getPosition().y));

	if (h.blockOrWall)
	{
		if (!cont)
		{
			BlockStruct b{h.block_id};
			m_world->setBlockWithNotify(h.x, h.y, b);
		}
	}
	else
	{
		if (!cont)
			m_world->setWallWithNotify(h.x, h.y, h.block_id);
	}

	// Send positive feedback that block was successfully changed
	BlockAck ack;
	ack.event_id = h.event_id;
	ack.gut = !cont;
	NetWriter(m.buffer).put(ack);
	m_tunnels.find(session)->second.write(m);

	if (!ack.gut)
	{
		ND_WARN("Could not place block on top of player");
		return;
	}

	// DONT! in million years: zero event id to hide what sender used, also this information is no longer necessary
	h.event_id = 789; //dont use zero
	NetWriter(m.buffer).put(h);
	for (auto& [s, tunnel] : m_tunnels)
		tunnel.write(m);
	ND_TRACE("BlockUpdate: ({},{}) id={} from session={}", h.x, h.y, h.block_id, session);
}

void ServerLayer::onMove(nd::net::Message& m)
{
	PlayerMoves h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("PlayerUpdate: Could not parse");
		return;
	}
	CHECK_SESSION_VALID();
	auto& info = m_player_book[h.session_id];
	info.updateLife();

	auto& moves = info.moves;
	if (moves.event_id >= h.event_id)
	{
		ND_WARN("onMove: Got input from before {} and current event_id is {}", h.event_id, moves.event_id);
		return; //i really dont care about input from before
	}
	moves.event_id = h.event_id;
	moves.targetPos = h.pos;
	m_log += "|Received:";
	m_log += "E:" + std::to_string(h.event_id);
	m_log += "P:" + glm::to_string(moves.targetPos);
	m_log += "I" + h.inputs.toString();
	m_log += "|";


	std::string s;
	for (auto& q : moves.inputs)
		s += q.toString() + ",";

	moves.inputs.push_back(h.inputs);
	//ND_TRACE("onMove: Adding input {} into inputs ({})", h.inputs.toString(), s);
}


// ===============COMMANDS===========================

void ServerLayer::scheduleStop(bool exit_or_restart)
{
	if (exit_or_restart)
		m_should_exit = true;
	else m_should_restart = true;
	m_death_counter = DEATH_COUNTER_TICKS;
}

void ServerLayer::disconnectClient(nd::net::Message& message, SessionID id, std::string_view reason, bool sendMessage)
{
	if (!m_player_book.contains(id))
		return;
	ND_INFO("Disconnecting:  {} with session {}", m_player_book[id].name, id);

	if (sendMessage && m_player_book[id].isValid)
	{
		ControlProtocol c;
		c.session_id = id;
		c.message = reason;
		NetWriter(message.buffer).put(c);
		message.address = m_player_book[id].address;
		send(m_socket, message);
		ND_TRACE("Disconnecting: sending disconnect packet");
	}

	auto iter = m_player_book.find(id);
	m_session_ids.erase(m_session_ids.find(iter->second.name));
	if (m_tunnels.find(id) != m_tunnels.end())
		m_tunnels.erase(m_tunnels.find(id));

	m_world->destroyPlayer(iter->second.session_id);
	m_player_book.erase(iter);
}

void ServerLayer::disconnectAll(nd::net::Message& message, std::string_view reason)
{
	auto copy = m_session_ids;
	for (auto& [smth, id] : copy)
		disconnectClient(message, id, reason);
}

void ServerLayer::sendClusterPong(Address a)
{
	Message m;
	m.address = a;

	ProtocolHeader h;
	h.action = Prot::ClusterPong;

	auto w = NetWriter(m.buffer);
	w.put(h);
	w.put(m_name);

	send(m_socket, m);
}

void ServerLayer::flushTCP()
{
	for (auto& [session,tun] : m_tunnels)
		tun.flush(m_big_udp_message);
}

void ServerLayer::broadcastCommand(nd::net::Message& m, const std::string& sender, const std::string& cmd,
                                   SessionID except)
{
	CommandProtocol h;
	h.action = Prot::Command;
	h.message = "[" + sender + "] " + cmd;


	for (auto& [session,info] : m_player_book)
	{
		if (!info.isValid)
			return;
		if (session == except)
			continue;
		h.session_id = session;
		NetWriter(m.buffer).put(h);
		m.address = info.address;
		m_tunnels.find(session)->second.write(m);
	}
}


void ServerLayer::sendCommand(nd::net::Message& m, SessionID id, const std::string& sender, const std::string& cmd)
{
	CommandProtocol h;
	h.action = Prot::Command;
	h.message = "[" + sender + "] " + cmd;
	h.session_id = id;
	NetWriter(m.buffer).put(h);
	m.address = m_player_book[id].address;
	m_tunnels.find(id)->second.write(m);
}

void ServerLayer::prepareMove()
{
	//ND_TRACE("PreparingMoves");
	for (auto& [session, info] : m_player_book)
	{
		if (!info.isValid)
			return;

		// insert empty input representing no presses
		//info.moves.inputs.push_back({});
	}
}

void ServerLayer::applyMoves()
{
	//ND_TRACE("Applying moves");
	for (auto& [session, info] : m_player_book)
	{
		if (!info.isValid)
			continue;

		auto player = m_world->getPlayer(session);
		if (!player)
			continue;

		if (info.moves.inputs.empty())
			continue;

		std::string s;
		for (auto& q : info.moves.inputs)
			s += q.toString() + ",";

		//todo figure out this madness
		//todo add comments to rewind as well, check who on earth is causing these rewinds every tick
		//todo have a niceday , pun intended

		//ND_TRACE("applying input {} from set ({}) to player with pos {}",
		//         info.moves.inputs[info.moves.inputs.size() - 1].toString(), s, player->getPosition());
		for (int i = 0; i < info.moves.inputs.size(); ++i)
		{
			applyMove(*player, info.moves.inputs[i]);
			player->update(*m_world);
			m_log += "!" + glm::to_string(player->getPosition());
		}
		m_log += "!";
		//ND_TRACE("After apply player has pos {}", player->getPosition());
	}
}

void ServerLayer::applyMove(EntityPlayer& player, Inputs& inputs)
{
	glm::vec2 accel(0, 0);
	accel.y = -9.8f / 60 / 4;

	auto& velocity = player.getVelocity();
	auto& pos = player.getPosition();


	float acc = 0.3f;
	float moveThroughBlockSpeed = 6;

	auto istsunderBlock = !m_world->isAir((int)pos.x, (int)(pos.y - 1));

	if (player.isFlying())
	{
		velocity.x = 0;
		velocity.y = 0;

		bool b = inputs.down | inputs.left | inputs.up | inputs.right;

		if (inputs.right)
			velocity.x = moveThroughBlockSpeed;
		if (inputs.left)
			velocity.x = -moveThroughBlockSpeed;
		if (inputs.up)
			velocity.y = moveThroughBlockSpeed;
		if (inputs.down)
			velocity.y = -moveThroughBlockSpeed;
	}
	else
	{
		if (inputs.right)
			accel.x = acc;
		if (inputs.left)
			accel.x = -acc;
		if (inputs.up)
		{
			if (istsunderBlock)
				velocity.y = 1;
		}
		player.getAcceleration() = accel;
	}
}

void ServerLayer::sendMoves(Message& m)
{
	PlayersMoved moved;
	for (auto& [session, info] : m_player_book)
	{
		if (!info.isValid)
			continue;
		info.moves.name = info.name;
		// get updated final position of player that updated after applying last Input
		info.moves.targetPos = m_world->getPlayer(session)->getPosition();

		m_log += "Sending:";
		m_log += "|E:";
		m_log += std::to_string(info.moves.event_id);
		m_log += "|";
		for (auto& q : info.moves.inputs)
			m_log += q.toString() + ",";

		// if not a single input was received by server in time, add one empty 
		if (info.moves.inputs.empty())
			info.moves.inputs.push_back({});

		moved.moves.push_back(info.moves);
		info.moves.inputs.clear();
	}


	NetWriter(m.buffer).put(moved);
	for (auto& [session,info] : m_player_book)
	{
		if (!info.isValid)
			continue;
		m.address = info.address;
		send(m_socket, m);
	}
}
