#pragma once
#include "ndpch.h"

#include "ServerCluster.h"
#include "layer/Layer.h"
#include "net/net.h"

class ServerLayer;


constexpr int WORLD_SERVER_0_PORT = 1230;
constexpr int WORLD_SERVER_1_PORT = 1231;


constexpr const char* OVERWORLD = "overworld";
constexpr const char* NETHER = "nether";

constexpr const char* GATE_WAY = OVERWORLD;

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
		std::string serverName=GATE_WAY;
		bool isAlive() const;
		void updateLife();


		PlayerInfo();
	};

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

	void pingClusters();
	void checkTimeout();

	void createBasicServers();

	void onInvReq(nd::net::Message& m);
	void onInvitation(nd::net::Message& m);
	void onClusterPong(nd::net::Message& m);

	bool m_close_pending=false;
	bool isClosePending() const { return m_close_pending; }


};
