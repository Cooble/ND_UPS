#pragma once
#include "ndpch.h"
#include <atomic>
#include <map>
#include <thread>

class ServerLayer;


class ServerCluster
{
private:
	int TPS = 10;

	struct ServerInstance
	{
		ServerLayer* server;
		std::thread thread;

		std::atomic<bool> closePending = false;
	};

	std::map<nd::Strid, std::shared_ptr<ServerInstance>> m_servers;


	void serverLoop(std::shared_ptr<ServerInstance> server);
public:
	void startServer(ServerLayer* server);
	void stopServer(ServerLayer* server);

	~ServerCluster();
};
