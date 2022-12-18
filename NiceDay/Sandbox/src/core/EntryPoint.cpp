#include "layer/LobbyServerLayer.h"
#include "layer/server_constants.h"
#include "net/net.h"
#include "net/net_iterator.h"
#include "world/WorldIO.h"
#include <signal.h>

#ifndef ND_TEST


// To enable tcp tunnel testing uncomment following line
//#define TEST_TCP_TUNNEL

#ifdef TEST_TCP_TUNNEL
#include "TestTCPTunnel.h"
#endif

static volatile bool sigint = false;
static void signal_callback_handler(int)
{
	std::cout << "Catch sigint"<<std::endl;
	sigint = true;
}


int main()
{
	signal(SIGINT, signal_callback_handler);

	using namespace nd;
	Log::init();

#ifdef TEST_TCP_TUNNEL
	TestTCPTunnel::test();
	return 0;
#endif


	LobbyServerLayer layer;

	layer.onAttach();


	size_t millisPerTick = server_const::TPS_MILLIS;

	while (!layer.isClosePending() && !sigint)
	{
		auto time = nowTime();
		layer.onUpdate();
		auto updateTime = nowTime() - time;

		//take a break for the rest of a tick
		auto sleepFor = millisPerTick - updateTime;
		if (millisPerTick > updateTime)
			std::this_thread::sleep_for(std::chrono::milliseconds(sleepFor));
	}
	layer.onDetach();
	//std::cin.get();

	Log::flush();
	return 0;
}
#endif
