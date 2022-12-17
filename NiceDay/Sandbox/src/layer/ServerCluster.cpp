#include "ServerCluster.h"

#include "ServerLayer.h"
#include "server_constants.h"

ServerCluster::ServerInstance::~ServerInstance()
{
	thread.join();
}


/* Not the best optimized solution for exact sleep, but its better than nothing */
static void exact_sleep_for(double dt)
{
	typedef std::chrono::high_resolution_clock clocki;

	static constexpr std::chrono::duration<double> MinSleepDuration(0);
	clocki::time_point start = clocki::now();
	while (std::chrono::duration<double>(clocki::now() - start).count() < dt) {
		std::this_thread::sleep_for(MinSleepDuration);
	}
}

void ServerCluster::serverLoop(std::shared_ptr<ServerInstance> server)
{
	size_t millisPerTick = server_const::TPS_MILLIS;

	do
	{
		server->server->onAttach();
		while (!server->closePending && !server->server->shouldExit() && !server->server->shouldRestart())
		{
			auto time = nd::nowTime();
			server->server->onUpdate();
			auto updateTime = nd::nowTime() - time;

			//take a break for the rest of a tick
			auto sleepFor = millisPerTick - updateTime;



			auto n = nd::nowTime();
			if (millisPerTick > updateTime) {
				//std::this_thread::sleep_for(std::chrono::milliseconds(sleepFor));
				exact_sleep_for(sleepFor/1000.);
			}
		}
		server->server->onDetach();
	}
	while (server->server->shouldRestart());

	m_to_remove.push_back(server);
	m_servers.erase(m_servers.find(SID(server->server->getName())));
	delete server->server;
	server->server = nullptr;
}

void ServerCluster::startServer(ServerLayer* server)
{
	auto instance = std::make_shared<ServerInstance>();
	m_servers[SID(server->getName())] = instance;

	instance->server = server;
	instance->thread = std::thread([this, instance] { serverLoop(instance); });
}

void ServerCluster::update()
{
	while (!m_to_remove.empty())
		if (!m_to_remove[0]->server)
			m_to_remove.erase(m_to_remove.begin());
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
