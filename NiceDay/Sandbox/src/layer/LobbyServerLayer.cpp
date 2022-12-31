#include "LobbyServerLayer.h"

#include "Prot.h"
#include "ServerLayer.h"
#include "server_constants.h"
#include "event/KeyEvent.h"

#include "net/net.h"
#include "net/net_iterator.h"
#include "world/nd_registry.h"

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
		if (!createBasicServers())
			throw std::exception();
	}
	catch (...)
	{
		ND_ERROR("Lobby: Error encountered during parsing settings.json, remove settings.json before starting server.");
		m_close_pending = true;
		return;
	}
	auto info = nd::net::CreateSocketInfo();
	info.async = true;
	info.port = m_address.port();
	if (createSocket(m_socket, info) != NetResponseFlags_Success)
	{
		ND_ERROR("Lobby: Could not open socket");
		m_close_pending = true;
		return;
	}
}

void LobbyServerLayer::onDetach()
{
	deInit();
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
		ND_INFO("RCEIVED");
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
		case Prot::Error:
			onError(m);
			break;
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


static int int_val(const json& j,const char* c,int defaultVal)
{
	if (!j.contains(c))
		return defaultVal;
	json a = j[c];
	if (!a.is_number_unsigned())
		return defaultVal;
	uint64_t v = a;
	if((uint64_t)(uint32_t)v!=v)
		return defaultVal;
	return a;

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
				"port": 1230,
				"playerLimit":0
			},
		"nether":{
				"port": 1231,
				"playerLimit":0
			}
	}
}
)";
		std::ofstream o("settings.json");
		o << f;
		ND_INFO("Lobby: JSON Settings file not found, creating one.");
	}

	std::ifstream i("settings.json");
	json j;
	i >> j;

	m_address = nd::net::Address(j.value("ip", "127.0.0.1"), int_val(j,"port",server_const::LOBBY_PORT));
	if (!m_address.isValid() || m_address.isPortReserved())
	{
		ND_ERROR("Lobby: Settings parsing failed invalid format or invalid port");
		return false;
	}
	ND_INFO("Lobby server settings loaded: {}", m_address.toString());

	std::vector<std::tuple<std::string, nd::net::Address,int>> servers;

	auto& worlds = j["worlds"];
	if (worlds.empty())
	{
		servers.emplace_back("overworld", nd::net::Address("0.0.0.0", WORLD_SERVER_0_PORT),0);
	}
	else
		for (auto& w : worlds.items())
		{
			auto name = w.key();
			int port = int_val(w.value(),"port", 0);
			auto ad = nd::net::Address(m_address.ip(), port);
			if (!ad.isValid() || ad.isPortReserved())
			{
				ND_ERROR("Invalid port number encountered during settings load. {}",name);
				return false;
			}
			auto playerLimit = int_val(w.value(),"playerLimit", 0);
			servers.emplace_back(name, ad,playerLimit);
		}

	for (auto& [string,address,playerLimit] : servers)
	{
		ND_INFO("Lobby: Creating Server: {}", address.toString());
		m_cluster.startServer(new ServerLayer(string, address.port(), m_address, playerLimit));
	}
	// dont forget to set first server as gateway
	GATEWAY = std::get<0>(servers[0]);
	return true;
}

// redirect request to world server
void LobbyServerLayer::onInvReq(nd::net::Message& m)
{
	EstablishConnectionProtocol h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("Lobby: InvREQ: Could not parse");
		return;
	}

	// check player name size
	if (h.player.size() < server_const::MIN_PLAYER_NAME_LENGTH
		|| h.player.size() > server_const::MAX_PLAYER_NAME_LENGTH)
	{
		ND_WARN("Lobby: InvREQ: Attempt with longer name: {}", h.player);
		sendError(m, h.player, "Refused to connect: Invalid name");
		return;
	}

	PlayerInfo& playerInfo = m_player_book[h.player];
	playerInfo.address = m.address; // save sender address

	// force server address only if sender is server
	if (isServerAddress(m.address))
	{
		if (m_server_book.contains(h.server) && m_server_book[h.server].isAlive())
		{
			playerInfo.serverName = h.server;
		}
		else
		{
			sendError(m, h.player, "Refused to connect: Server does not exist");
			return;
		}
	}

	// if server does not exist, set gateway
	if (!m_server_book.contains(playerInfo.serverName) || !m_server_book[playerInfo.serverName].isAlive())
	{
		ND_INFO("Lobby: InvREQ: Could not find alive server {}, choosing GATEWAY: {} instead", playerInfo.serverName,
		        GATEWAY);
		playerInfo.serverName = GATEWAY;
	}

	// if finally the server is not alive
	if (!m_server_book.contains(playerInfo.serverName) || !m_server_book[playerInfo.serverName].isAlive())
	{
		ND_WARN("Lobby: InvREQ: Requested server/GATEWAY not running, searching through all servers");

		playerInfo.serverName = "";
		for (auto p:m_server_book)
			if(p.second.isAlive())
			{
				playerInfo.serverName = p.first;
				ND_INFO("Found server that is alive! {}", p.first);
				break;
			}

		if (playerInfo.serverName.empty()) {
			sendError(m, h.player, "Refused to connect: No servers running");
			ND_WARN("Lobby: InvREQ: No servers are running");
			return;
		}
	}

	// jesus at last redirect request to server
	NetWriter(m.buffer).put(h);
	m.address = m_server_book[playerInfo.serverName].address;
	nd::net::send(m_socket, m);
}

void LobbyServerLayer::onInvitation(nd::net::Message& m)
{
	EstablishConnectionProtocol h;

	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("Lobby: Inv: Could not parse");
		return;
	}

	if (!m_player_book.contains(h.player))
	{
		ND_WARN("Lobby: Inv: Received invitation from server for player {} who has already timed out", h.player);
		return;
	}

	PlayerInfo& playerInfo = m_player_book[h.player];

	auto adr = m.address.toString();
	//send same ip as lobby server is in, works only if other servers are localhost
	h.server = Address(m_address.ip(), m.address.port()).toString();

	NetWriter(m.buffer).put(h);
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

void LobbyServerLayer::onError(nd::net::Message& m)
{
	// redirect only errors from servers
	if (!isServerAddress(m.address))
		return;

	ErrorProtocol h;
	if (!NetReader(m.buffer).get(h))
	{
		ND_WARN("Lobby: ErrorProtocol: Could not parse");
		return;
	}

	if (m_player_book.contains(h.player))
	{
		m.address = m_player_book[h.player].address;
		send(m_socket, m);
		ND_TRACE("Lobby: Redirecting error from server to client");
	}
	else
		ND_TRACE("Lobby: Cannot redirect error, client not found");
}

bool LobbyServerLayer::isServerAddress(nd::net::Address a) const
{
	for (auto& s : m_server_book)
		if (s.second.address == a)
			return true;
	return false;
}

void LobbyServerLayer::sendError(nd::net::Message& m, const std::string& player, const std::string& reason)
{
	ErrorProtocol p;
	p.player = player;
	p.reason = reason;
	NetWriter(m.buffer).put(p);
	send(m_socket, m);
}
