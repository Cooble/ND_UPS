#include "ServerLayer.h"

#include "Prot.h"
#include "server_constants.h"
#include "net/Multiplayer.h"
#include "world/nd_registry.h"
#include "world/World.h"


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
	info.port = port;
	createSocket(m_socket, info);
}

void ServerLayer::closeSocket()
{
	nd::net::closeSocket(m_socket);
}

ServerLayer::SessionID ServerLayer::allocateNewSessionID()
{
	return ++lastId;
}

ServerLayer::ServerLayer(const std::string& name, int port,
                         nd::net::Address lobbyAddress): m_lobby_address(lobbyAddress), port(port)
{
	m_name = name;
	m_big_udp_message.buffer.reserve(1500);
}

void ServerLayer::onAttach()
{
	m_should_restart = false;
	ND_TRACE("Starting server on port {}", port);
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

void ServerLayer::onInvReq(nd::net::Message& m)
{
	auto reader = NetReader(m.buffer);

	EstablishConnectionProtocol h;
	if (!reader.get(h))
		return;

	ND_TRACE("Invitation Request Received from {}", h.player);

	if (m_session_ids.contains(h.player))
		return; //already here

	SessionID newId = allocateNewSessionID();
	m_session_ids[h.player] = newId;
	auto& info = m_player_book[newId];
	info.session_id = newId;
	info.isValid = false;
	info.name = h.player;

	h.action = Prot::Invitation;
	h.server = m_name;
	h.session_id = info.session_id;

	NetWriter(m.buffer).put(h);
	send(m_socket, m);
}

void ServerLayer::onInvACK(nd::net::Message& m)
{
	EstablishConnectionProtocol h;
	if (!NetReader(m.buffer).get(h))
		return;

	// invitation does not exist
	if (!m_session_ids.contains(h.player))
		return;

	auto& info = m_player_book[m_session_ids[h.player]];

	// invitation id does not match
	if (h.session_id != info.session_id)
		return;

	if (!info.isValid)
	{
		//create tcp tunnel
		m_tunnels.try_emplace(info.session_id, m_socket, m.address, info.session_id, 5000);
	}
	// mark player state (re)verified
	info.address = m.address;
	info.isValid = true;
	info.updateLife();
	ND_INFO("Client: {} obtained session ID: {}", m.address.toString(), info.session_id);

	h.action = Prot::SessionCreated;
	h.session_id = info.session_id;

	NetWriter(m.buffer).put(h);
	send(m_socket, m);
	sendCommand(m, info.session_id, "Hello " + info.name + "! Welcome to the server " + m_name);
}

void ServerLayer::onChunkReq(nd::net::Message& m)
{
	ChunkProtocol h;
	if (!NetReader(m.buffer).get(h))
		return;

	// imposter without session id
	if (!m_player_book.contains(h.session_id))
		return;

	ND_TRACE("Chunk Req Received from {},{}, piece={},session={}", h.c_x, h.c_y, h.piece, h.session_id);

	auto chunkPtr = m_world->getChunkM(h.c_x, h.c_y);
	if (!chunkPtr)
		return;

	auto& mail = m_chunk_mail[h.session_id];

	ND_TRACE("Chunk REQ received: {},{}", h.c_x, h.c_y);

	if (mail.c_id != chunkPtr->chunkID())
	{
		mail.fillWithChunk(chunkPtr);
	}
	else if (h.piece >= 0 && h.piece < CHUNK_PIECE_COUNT)
	{
		ND_TRACE("Chunk REQ received for one piece received: {},{}, piece ={} ", h.c_x, h.c_y, (int)h.piece);
		mail.pushPendingPiece(h.piece);
	}
}

void ServerLayer::onPlayerUpdate(nd::net::Message& m)
{
	PlayerProtocol h;
	if (!NetReader(m.buffer).get(h))
		return;

	// imposter without session id
	if (!m_player_book.contains(h.session_id))
		return;

	if (!h.block_modifies.empty())
		ND_TRACE("Got block modify events from player {}", m_player_book[h.session_id].name);
	for (auto& [block_id, blockOrWall, pos] : h.block_modifies)
	{
		if (blockOrWall)
		{
			BlockStruct b{block_id};
			m_world->setBlockWithNotify(pos.x, pos.y, b);
		}
		else
		{
			m_world->setWallWithNotify(pos.x, pos.y, block_id);
		}
	}
}

void ServerLayer::onCommand(nd::net::Message& m)
{
	CommandProtocol h;
	if (!NetReader(m.buffer).get(h))
		return;

	// imposter without session id
	if (!m_player_book.contains(h.session_id))
		return;

	ND_INFO("Recevied command: {}", h.message);

	if (nd::SUtil::startsWith(h.message, '/'))
	{
		auto cmd = h.message.substr(1);
		if (cmd == "exit")
		{
			ND_INFO("Received exit command");
			m_should_exit = true;
			broadcastCommand(m, "Server exiting, bye bye");
		}
		else if (cmd == "restart")
		{
			ND_INFO("Received restart command");
			broadcastCommand(m, "Server restarting, bye bye");
			m_should_restart = true;
		}
		else
		{
			ND_INFO("Received invalid command");
			sendCommand(m, h.session_id, "Invalid command!");
		}
	}
}

void ServerLayer::onUpdate()
{
	if (!m_is_online)
		cryForAttention();
	checkTimeout();
	sendChunks();

	Message& m = m_big_udp_message;

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
		case Prot::PlayerUpdate:
			onPlayerUpdate(m);
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
		default: ;
		}
	}

	//tcp poll
	for (auto& [session,tun] : m_tunnels)
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
			}
		}
	}
	tcpFlush();
}

void ServerLayer::checkTimeout()
{
	//throw away dead players
	auto iter = m_player_book.begin();
	while (iter != m_player_book.end())
	{
		if (!iter->second.isAlive())
		{
			ND_INFO("Client {} timed out!", iter->first);
			auto name = iter->second.name;
			iter = m_player_book.erase(iter);
			m_session_ids.erase(m_session_ids.find(name));
		}
		else
		{
			++iter;
		}
	}
	// do not throw away dead servers though
}

void ServerLayer::onDetach()
{
	closeSocket();
	m_world->saveWorld();
	delete m_world;
}

void ServerLayer::cryForAttention()
{
	if (m_ticks_until_cry-- != 0)
		return;
	m_ticks_until_cry = server_const::CRY_FOR_ATTENTION_INTERVAL;

	sendClusterPong(m_lobby_address);
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

constexpr int CHUNK_PIECES_PER_TICK = 40;

void ServerLayer::sendChunks()
{
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

			ND_TRACE("Sending ChunkPiece {} to {} with length={}", (int)piece, m_player_book[session].name, len);
			mes.address = m_player_book[session].address;
			send(m_socket, mes);
		}
	}
}

void ServerLayer::tcpFlush()
{
	for (auto& [session,tun] : m_tunnels)
		tun.flush(m_big_udp_message);
}

void ServerLayer::broadcastCommand(nd::net::Message& m, const std::string& cmd)
{
	CommandProtocol h;
	h.action = Prot::Command;
	h.message = "[server] " + cmd;

	for (auto& [session,info] : m_player_book)
	{
		if (!info.isValid)
			return;
		h.session_id = session;
		NetWriter(m.buffer).put(h);
		m.address = info.address;
		m_tunnels.find(session)->second.write(m);
	}
}

void ServerLayer::sendCommand(nd::net::Message& m, SessionID id, const std::string& cmd)
{
	CommandProtocol h;
	h.action = Prot::Command;
	h.message = "[server] " + cmd;
	h.session_id = id;
	NetWriter(m.buffer).put(h);
	m.address = m_player_book[id].address;
	m_tunnels.find(id)->second.write(m);
}
