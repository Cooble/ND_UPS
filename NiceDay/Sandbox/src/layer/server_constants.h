#pragma once
namespace server_const
{
	constexpr int TPS=60;
	constexpr int TPS_MILLIS = 1000/ TPS;


	// in ticks interval between sending i am alive to lobby from server
	constexpr int CRY_FOR_ATTENTION_INTERVAL = 2;

	//how long player info will be held on lobby
	constexpr size_t PLAYER_LOBBY_TIMEOUT = 5000;

	//how long until server is deemed offline by lobby
	constexpr size_t SERVER_LOBBY_TIMEOUT = 20000;

	// time between pinging servers by lobby
	constexpr size_t PING_INTERVAL = 4000/TPS_MILLIS;

	constexpr int LOBBY_PORT = 1234;

	// player kicked if no response
	constexpr size_t PLAYER_SERVER_TIMEOUT=2000;
}
