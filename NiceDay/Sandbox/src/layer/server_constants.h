#pragma once
namespace server_const
{
#define N_TIMEOUT(val)\
	server_const::DISABLE_TIMEOUT?1000000000:(val)


constexpr bool DISABLE_TIMEOUT = true;

constexpr int TPS = 30;
constexpr int TPS_MILLIS = DISABLE_TIMEOUT ? (5000) : 1000 / TPS;


// in ticks interval between sending i am alive to lobby from server
constexpr int CRY_FOR_ATTENTION_INTERVAL = 10;

//how long player info will be held on lobby
constexpr size_t PLAYER_LOBBY_TIMEOUT = N_TIMEOUT(5000);

//how long until server is deemed offline by lobby
constexpr size_t SERVER_LOBBY_TIMEOUT = N_TIMEOUT(20000);

// time between pinging servers by lobby
constexpr size_t PING_INTERVAL = 4000 / TPS_MILLIS;

constexpr int LOBBY_PORT = 1234;

constexpr bool DISABLE_MOVE = false;

// player kicked if no response
constexpr size_t PLAYER_SERVER_TIMEOUT = N_TIMEOUT(DISABLE_MOVE ? 100000000 : 5000);

constexpr int MAX_PLAYER_NAME_LENGTH = 11;
constexpr int MIN_PLAYER_NAME_LENGTH = 3;

//any command longer than this will be silently discarded
constexpr size_t MAX_COMMAND_MESSAGE_LENGTH = 300;
constexpr size_t HOP_TIMEOUT = N_TIMEOUT(2000);
}
