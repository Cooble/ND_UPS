#pragma once
#include "ndpch.h"
#include <atomic>
#include <map>
#include <thread>

class ServerLayer;


class ServerCluster
{
private:
	int TPS = 30;

	struct ServerInstance
	{
		ServerLayer* server;
		std::thread thread;

		std::atomic<bool> closePending = false;

		~ServerInstance();
	};

	std::map<nd::Strid, std::shared_ptr<ServerInstance>> m_servers;
	std::vector<std::shared_ptr<ServerInstance>> m_to_remove;


	void serverLoop(std::shared_ptr<ServerInstance> server);
public:
	void startServer(ServerLayer* server);
	void update();
	void stopServer(ServerLayer* server);

	~ServerCluster();
};
