#include "layer/LobbyServerLayer.h"
#include "layer/server_constants.h"
#include "world/WorldIO.h"
#ifndef ND_TEST
int main()
{
	using namespace nd;
	
	Log::init();
	
	LobbyServerLayer layer;

	layer.onAttach();


	size_t millisPerTick = 1000 / server_const::TPS;

	while (!layer.isClosePending())
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
