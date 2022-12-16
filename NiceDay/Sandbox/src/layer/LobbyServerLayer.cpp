#include "LobbyServerLayer.h"

#include "Prot.h"
#include "ServerLayer.h"
#include "server_constants.h"
#include "event/KeyEvent.h"
#include "net/Multiplayer.h"

#include "net/net.h"
#include "net/net_iterator.h"
#include "world/nd_registry.h"
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
	nd::ResourceMan::init();
	nd_registry::registerEverything();
	nd::net::init();

	try
	{
		createBasicServers();
	}
	catch (...)
	{
		ND_ERROR("Error encountered during parsing settings.json, remove settings.json before starting server.");
		m_close_pending = true;
		return;
	}
	auto info = nd::net::CreateSocketInfo();
	info.async = true;
	info.port = m_address.port();
	createSocket(m_socket, info);
}

void LobbyServerLayer::onDetach()
{
	deinit();
}

void LobbyServerLayer::onEvent(nd::Event& e)
{
}

void LobbyServerLayer::onUpdate()
{
	m_cluster.update();
	pingClusters();
	checkTimeout();

	Message m;
	m.buffer.reserve(1500);

	while (receive(m_socket, m) == NetResponseFlags_Success)
	{
		ProtocolHeader header;
		if (!NetReader(m.buffer).get(header))
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
		//ND_INFO("Ping");
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
	while (iter != m_player_book.end())
	{
		if (!iter->second.isAlive())
		{
			iter = m_player_book.erase(iter);
		}
		else
		{
			++iter;
		}
	}
	// do not throw away dead servers though
}


bool LobbyServerLayer::createBasicServers()
{
	if (!std::filesystem::exists("settings.json"))
	{
		std::string f = R"(
{
    "port": 1234,
    "ip": "127.0.0.1",
	"worlds":{
		"overworld":{
				"port": 1230
			},
		"nether":{
				"port": 1231
			}
	}
}
)";
		std::ofstream o("settings.json");
		o << f;
		ND_INFO("JSON Settings file not found, creating one.");
	}

	std::ifstream i("settings.json");
	json j;
	i >> j;

	m_address = nd::net::Address(j.value("ip", "127.0.0.1"), j.value("port", server_const::LOBBY_PORT));
	if (!m_address.isValid())
	{
		ND_ERROR("Settings parsing failed invalid format");
		return false;
	}
	ND_INFO("Lobby server settings loaded: {}", m_address.toString());

	std::vector<std::pair<std::string, nd::net::Address>> servers;

	auto& worlds = j["worlds"];
	if (worlds.empty())
	{
		servers.emplace_back("overworld", nd::net::Address("0.0.0.0", WORLD_SERVER_0_PORT));
	}
	else
		for (auto& w : worlds.items())
		{
			auto name = w.key();
			int port = w.value().value("port", 0);
			if (!nd::net::Address("127.0.0.1", port).isValid())
			{
				ND_ERROR("Invalid port number encountered during settings load.");
				return false;
			}
			servers.emplace_back(name, nd::net::Address("0.0.0.0", port));
		}

	for (auto& [string,address] : servers)
	{
		ND_INFO("Creating Server: {} on port {}", string, address.port());
		m_cluster.startServer(new ServerLayer(string, address.port(), m_address));
	}
	// dont forget to set first server as gateway
	GATEWAY = servers[0].first;
	return true;
}

// redirect request to world server
void LobbyServerLayer::onInvReq(nd::net::Message& m)
{
	EstablishConnectionProtocol playerHeader;
	if (!NetReader(m.buffer).get(playerHeader))
	{
		ND_WARN("InvREQ: Could not parse");
		return;
	}

	if (playerHeader.player.size() < server_const::MIN_PLAYER_NAME_LENGTH
		|| playerHeader.player.size() > server_const::MAX_PLAYER_NAME_LENGTH)
	{
		ND_WARN("InvREQ: Attempt with longer name: {}", playerHeader.player);
		return;
	}

	PlayerInfo& playerInfo = m_player_book[playerHeader.player];
	playerInfo.address = m.address; // save sender address
	if (!m_server_book.contains(playerInfo.serverName) || !m_server_book[playerInfo.serverName].isAlive())
	{
		ND_INFO("InvREQ: Could not find alive server {}, choosing GATEWAY: {} instead", playerInfo.serverName, GATEWAY);
		playerInfo.serverName = GATEWAY;
	}

	if (!m_server_book[playerInfo.serverName].isAlive())
	{
		ND_WARN("InvREQ: Cannot respond: No servers running, not even gateway");
		return;
	}
	NetWriter(m.buffer).put(playerHeader);
	m.address = m_server_book[playerInfo.serverName].address;
	nd::net::send(m_socket, m);
}

void LobbyServerLayer::onInvitation(nd::net::Message& m)
{
	EstablishConnectionProtocol playerHeader;

	if (!NetReader(m.buffer).get(playerHeader))
	{
		ND_WARN("Inv: Could not parse");
		return;
	}

	if (!m_player_book.contains(playerHeader.player))
	{
		ND_WARN("Inv: Received invitation from server for player {} who has already timed out", playerHeader.player);
		return;
	}

	PlayerInfo& playerInfo = m_player_book[playerHeader.player];

	auto adr = m.address.toString();
	playerHeader.server = m.address.toString();

	NetWriter(m.buffer).put(playerHeader);
	m.address = playerInfo.address;
	nd::net::send(m_socket, m);
}

void LobbyServerLayer::onClusterPong(nd::net::Message& m)
{
	ProtocolHeader h;

	auto reader = NetReader(m.buffer);
	reader.get(h);

	auto serverName = reader.readStringUntilNull(32);

	auto& serverInfo = m_server_book[serverName];
	m_server_book[serverName].address = m.address;
	serverInfo.updateLife();
}
