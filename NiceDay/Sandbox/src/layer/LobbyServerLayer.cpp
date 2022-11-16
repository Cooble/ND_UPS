#include "LobbyServerLayer.h"

#include <WinSock2.h>

#include "Prot.h"
#include "ServerLayer.h"
#include "server_constants.h"
#include "event/KeyEvent.h"
#include "net/Multiplayer.h"

#include "net/net.h"
#include "world/block/Block.h"

using namespace nd::net;


bool LobbyServerLayer::ServerInfo::isAlive() const
{
	return nd::nowTime() - lastSeen < server_const::SERVER_LOBBY_TIMEOUT;
}

void LobbyServerLayer::ServerInfo::updateLife()
{
	lastSeen = nd::nowTime();
}

LobbyServerLayer::ServerInfo::ServerInfo()
{
	updateLife();
}

bool LobbyServerLayer::PlayerInfo::isAlive() const
{
	return nd::nowTime() - lastSeen < server_const::PLAYER_LOBBY_TIMEOUT;
}

void LobbyServerLayer::PlayerInfo::updateLife()
{
	lastSeen = nd::nowTime();
}

LobbyServerLayer::PlayerInfo::PlayerInfo()
{
	updateLife();
}

void LobbyServerLayer::onAttach()
{
	nd::net::init();
	auto info = nd::net::CreateSocketInfo();
	info.async = true;
	info.port = 1234;
	createSocket(m_socket, info);

	createBasicServers();

	int i = sizeof(BlockStruct);
	i = i;
}

void LobbyServerLayer::onDetach()
{
	deinit();
}

void LobbyServerLayer::onEvent(nd::Event& e)
{
	if (e.getEventType() == nd::Event::KeyPress)
	{
		auto& w = dynamic_cast<nd::KeyPressEvent&>(e);
		if (w.getFreshKey() == nd::KeyCode::S)
		{
			Message m;

			ProtocolHeader header;
			header.action = Prot::Ping;

			NetWriter(m.buffer).put(header);

			nd::net::send(m_socket, m);

			ND_INFO("Sending ping req");
		}
	}
}

void LobbyServerLayer::onUpdate()
{
	pingClusters();
	checkTimeout();

	Message m;
	m.buffer.reserve(1024);

	while (receive(m_socket, m) == NetResponseFlags_Success)
	{
		ProtocolHeader header;
		if(!NetReader(m.buffer).get(header))
			continue;

		switch (header.action)
		{
		case Prot::ERR_InvalidREQ:
			break;
		case Prot::InvitationREQ:
			onInvReq(m);
			break;
		case Prot::Invitation:
			onInvitation(m);
			break;

		case Prot::MigrateREQ: break;
		case Prot::MigrateWait: break;
		case Prot::MigratedACK: break;

		case Prot::Ping:
			header.action = Prot::Pong;
			NetWriter(m.buffer).put(header);
			nd::net::send(m_socket, m);
			break;
		case Prot::Pong:
			break;
		case Prot::ClusterPing: break;
		case Prot::ClusterPong:
			onClusterPong(m);
			break;
		default: ;
		}
	}
}

static int timeUntilPing = 0;

void LobbyServerLayer::pingClusters()
{
	if (timeUntilPing-- == 0)
	{
		timeUntilPing = server_const::PING_INTERVAL;
		ND_INFO("Ping");
		Message m;
		ProtocolHeader p;
		p.action = Prot::ClusterPing;
		NetWriter(m.buffer).put(p);

		for (auto& server : m_server_book)
		{
			m.address = server.second.address;
			nd::net::send(m_socket, m);
		}
	}
}

void LobbyServerLayer::checkTimeout()
{
	//throw away dead players
	auto iter = m_player_book.begin();
	while (iter != m_player_book.end()) {
		if (!iter->second.isAlive()) {
			iter = m_player_book.erase(iter);
		}
		else {
			++iter;
		}
	}
	// do not throw away dead servers though
}


void LobbyServerLayer::createBasicServers()
{
	m_address = Address("127.0.0.1", server_const::LOBBY_PORT);

	/*ServerInfo i0;
	ServerInfo i1;
	i0.address = Address(ADDR_ANY, WORLD_SERVER_0_PORT);
	i1.address = Address(ADDR_ANY, WORLD_SERVER_1_PORT);
	m_server_book[OVERWORLD] = i0;
	m_server_book[OVERWORLD] = i1;*/

	m_cluster.startServer(new ServerLayer(OVERWORLD, WORLD_SERVER_0_PORT, m_address));
	m_cluster.startServer(new ServerLayer(NETHER, WORLD_SERVER_1_PORT, m_address));
}

// redirect request to world server
void LobbyServerLayer::onInvReq(nd::net::Message& m)
{
	EstablishConnectionProtocol playerHeader;
	if (!NetReader(m.buffer).get(playerHeader))
		return;


	PlayerInfo& playerInfo = m_player_book[playerHeader.player];
	playerInfo.address = m.address; // save sender address

	ServerInfo& server = m_server_book[playerInfo.serverName];

	// server is dead lets try gateway instead
	if (!server.isAlive())
		playerInfo.serverName = GATE_WAY;


	NetWriter(m.buffer).put(playerHeader);
	m.address = server.address;
	nd::net::send(m_socket, m);
}

void LobbyServerLayer::onInvitation(nd::net::Message& m)
{
	EstablishConnectionProtocol playerHeader;

	if (!NetReader(m.buffer).get(playerHeader))
		return;


	PlayerInfo& playerInfo = m_player_book[playerHeader.player];

	auto adr = m.address.toString();
	playerHeader.server = m.address.toString();

	m.address = playerInfo.address;
	nd::net::send(m_socket, m);
}

void LobbyServerLayer::onClusterPong(nd::net::Message& m)
{
	ProtocolHeader p;

	auto reader = NetReader(m.buffer);
	reader.get(p);

	auto serverName = reader.readStringUntilNull(32);

	auto& serverInfo = m_server_book[serverName];
	m_server_book[serverName].address = m.address;
	serverInfo.updateLife();
}
