#include "ServerLayer.h"

#include "Prot.h"
#include "server_constants.h"
#include "net/Multiplayer.h"
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
	for (char i = 0; i < 20; ++i)
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
}

void ServerLayer::onAttach()
{
	ND_TRACE("Starting server on port {}", port);
	openSocket();

	WorldInfo info = {};
	strcpy(info.name, m_name.c_str());
	info.chunk_height = 1;
	info.chunk_width = 1;
	info.terrain_level = 16;
	info.time = 0;
	m_world = new World(m_name, info);
	ND_INFO("Generating world {}", info.name);
}

void ServerLayer::onInvReq(nd::net::Message& m)
{
	auto reader = NetReader(m.buffer);

	EstablishConnectionProtocol header;
	if (!reader.get(header))
		return;

	if (m_session_ids.contains(header.player))
		return; //already here

	SessionID newId = allocateNewSessionID();
	m_session_ids[header.player] = newId;
	auto& info = m_player_book[newId];
	info.session_id = allocateNewSessionID();
	info.isValid = false;
	info.name = header.player;

	header.action = Prot::Invitation;
	header.server = m_name;
	header.session_id = info.session_id;

	NetWriter(m.buffer).put(header);
	send(m_socket, m);
}

void ServerLayer::onInvACK(nd::net::Message& m)
{
	EstablishConnectionProtocol header;
	if (!NetReader(m.buffer).get(header))
		return;

	// invitation does not exist
	if (!m_session_ids.contains(header.player))
		return;

	auto& info = m_player_book[m_session_ids[header.player]];

	// invitation id does not match
	if (header.session_id != info.session_id)
		return;

	// mark player state (re)verified
	info.isValid = true;
	info.updateLife();
	ND_INFO("Client: {} obtained session ID: {}", m.address.toString(), info.session_id);

	header.action = Prot::SessionCreated;
	header.session_id = info.session_id;

	NetWriter(m.buffer).put(header);
	send(m_socket, m);
}

void ServerLayer::onChunkReq(nd::net::Message& m)
{
	ChunkProtocol header;
	if (!NetReader(m.buffer).get(header))
		return;

	auto chunkPtr = m_world->getChunkM(header.c_x, header.c_y);
	if (!chunkPtr)
		return;

	auto& mail = m_chunk_mail[header.session_id];

	ND_TRACE("Chunk REQ received: {},{}", header.c_x, header.c_y);

	if (mail.c_id != chunkPtr->chunkID())
	{
		mail.fillWithChunk(chunkPtr);
	}
	else if (header.piece != -1)
	{
		//todo check piece range
		ND_TRACE("Chunk REQ received for one piece received: {},{}", header.c_x, header.c_y);
		mail.pushPendingPiece(header.piece);
	}
}

void ServerLayer::onUpdate()
{
	if (!m_is_online)
		cryForAttention();
	checkTimeout();
	sendChunks();

	Message m;
	m.buffer.reserve(1024);

	while (receive(m_socket, m) == NetResponseFlags_Success)
	{
		ProtocolHeader header;
		if (!NetReader(m.buffer).get(header))
			continue;

		switch (header.action)
		{
		case Prot::InvitationREQ:
			onInvReq(m);
			break;
		case Prot::InvitationACK:
			onInvACK(m);
			break;
		case Prot::Ping:
			header.action = Prot::Pong;
			NetWriter(m.buffer).put(header);
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
			iter = m_player_book.erase(iter);
			m_session_ids.erase(m_session_ids.find(iter->second.name));
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

	ProtocolHeader p;
	p.action = Prot::ClusterPong;

	auto w = NetWriter(m.buffer);
	w.put(p);
	w.put(m_name);

	send(m_socket, m);
}

void ServerLayer::sendChunks()
{
	Message mes;
	mes.buffer.reserve(CHUNK_PIECE_SIZE + 50);

	for(auto& [session, m] :m_chunk_mail)
	{
		if(m.pending())
		{
			auto piece = m.popPendingPiece();
			mes.address = m_player_book[session].address;

			auto ptr = m.pieceMemory(piece);
			auto len = m.pieceLength(piece);

			ChunkProtocol p;
			p.c_x = half_int::X(m.c_id);
			p.c_y = half_int::Y(m.c_id);
			p.piece = piece;
			p.session_id = session;

			auto writer = NetWriter(mes.buffer);
			writer.put(p);
			writer.write(ptr, len);

			ND_TRACE("Sending ChunkPiece {} to {}", piece, m_player_book[session].name);
			mes.address = m_player_book[session].address;
			send(m_socket, mes);
		}
	}
}
