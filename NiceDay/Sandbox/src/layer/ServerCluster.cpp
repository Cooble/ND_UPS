#include "ServerCluster.h"

#include "ServerLayer.h"

void ServerCluster::serverLoop(std::shared_ptr<ServerInstance> server)
{
	server->server->onAttach();


	size_t millisPerTick = 1000 / TPS;

	while (!server->closePending)
	{
		auto time = nd::nowTime();
		server->server->onUpdate();
		auto updateTime = nd::nowTime() - time;

		//take a break for the rest of a tick
		auto sleepFor = millisPerTick - updateTime;
		if (millisPerTick > updateTime)
			std::this_thread::sleep_for(std::chrono::milliseconds(sleepFor));
	}
	server->server->onDetach();

	m_servers.erase(m_servers.find(SID(server->server->getName())));
	delete server->server;
}

void ServerCluster::startServer(ServerLayer* server)
{
	auto instance = std::make_shared<ServerInstance>();
	m_servers[SID(server->getName())] = instance;

	instance->server = server;
	instance->thread = std::thread([this, instance] { serverLoop(instance); });
}

void ServerCluster::stopServer(ServerLayer* server)
{
	auto f = m_servers.find(SID(server->getName()));
	if (f != m_servers.end())
		f->second->closePending = true;
}

ServerCluster::~ServerCluster()
{
	for (auto& it : m_servers)
		it.second->closePending = true;

	for (auto& it : m_servers)
		it.second->thread.join();
}
