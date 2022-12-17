#pragma once
#include "ndpch.h"
#include "nlohmann/json.hpp"

#include "ServerCluster.h"
#include "core/NBT.h"
#include "layer/Layer.h"
#include "net/net.h"

class ServerLayer;


constexpr int WORLD_SERVER_0_PORT = 1230;


class LobbyServerLayer : public nd::Layer
{
private:
	struct ServerInfo
	{
		size_t lastSeen=0;
		nd::net::Address address;

		bool isAlive() const;
		void updateLife();

		ServerInfo();
	};

	struct PlayerInfo
	{
		size_t lastSeen=0;
		nd::net::Address address;
		std::string serverName;
		bool isAlive() const;
		void updateLife();


		PlayerInfo();
	};
	// default server to set traffic to
	std::string GATEWAY;
	// our lobby address
	nd::net::Address m_address;
	nd::net::Socket m_socket;
	ServerCluster m_cluster;
	nd::defaultable_map_other<std::string, PlayerInfo> m_player_book;
	nd::defaultable_map_other<std::string, ServerInfo> m_server_book;

public:
	void onAttach() override;
	void onDetach() override;
	void onEvent(nd::Event& e) override;
	void onUpdate() override;

	// ping servers to see if still alive
	void pingClusters();

	// remove old players
	void checkTimeout();

	// parse settings.json and start servers in server cluster
	bool createBasicServers();

	// client/server sent request for a server or specific server
	// redirect request to appropriate server
	void onInvReq(nd::net::Message& m);

	// redirect invitation from server to the client/other server
	void onInvitation(nd::net::Message& m);
	// accept response from server which is still alive
	void onClusterPong(nd::net::Message& m);
	// redirect error from server to client/other server
	void onError(nd::net::Message& m);

	bool m_close_pending=false;

	// should close in next tick
	bool isClosePending() const { return m_close_pending; }

	// address from one of our servers
	bool isServerAddress(nd::net::Address) const;
	// sends udp error duh
	void sendError(nd::net::Message&, const std::string& player,const std::string& reason);


};
