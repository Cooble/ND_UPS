#pragma once
#include <map>
#include <optional>

#include "Prot.h"
#include "layer/Layer.h"
#include "net/net.h"
#include "net/TCPTunnel.h"
#include "world/World.h"


class Chunk;
class World;

constexpr std::string_view MES_SERVER_KICK_RESTART = "Server restarting.";
constexpr std::string_view MES_SERVER_KICK_QUIT = "Server quitting. Don't try to connect again:)";
constexpr std::string_view MES_SERVER_KICK_KICKED = "You were disconnected because: ";
constexpr std::string_view MES_SERVER_KICK_TIMEOUT = "You were disconnected because: Timeout";
constexpr std::string_view MES_CLIENT_DISCONNECT = "CLIENT_DISCONNECT";

constexpr int DEATH_COUNTER_TICKS = 60;
// number of ticks before PlayersMoved packet is sent to all clients
constexpr int UPDATE_INTERVAL = 4;

class ServerLayer : public nd::Layer
{
public:
	//structs
	typedef int SessionID;
	struct PlayerInfo
	{
		// time of the last response from player 
		size_t lastSeen;
		nd::net::Address address;
		SessionID session_id = 0;

		// not valid until invAck is received
		bool isValid = false;

		std::string name;

		// time when hop was requested
		size_t hopTime=0;

		// move info about server update
		PlayerMoved moves0;
		PlayerMoved moves1;

		// which PlayerMoved is current is which is future
		bool moves_flip = false;

		auto& curMoves() { return moves_flip ? moves1 : moves0; }
		auto& nextMoves() { return moves_flip ? moves0 : moves1; }

		void flipMoves() { moves_flip ^= 1; }

		bool isAlive() const;
		void updateLife();
		PlayerInfo();
	};
	struct ChunkMail
	{
		ChunkID c_id = -1;

		bool being_loaded = false;
		std::vector<char> buffer;
		std::vector<char> pending_pieces;
		void makeVacant() { c_id = -1; }
		bool vacant() const { return c_id == -1; }
		bool pending() const { return !pending_pieces.empty(); }

		void fillWithChunk(Chunk* chunk);

		void pushPendingPiece(char c)
		{
			pending_pieces.push_back(c);
		}

		char popPendingPiece()
		{
			ASSERT(pending(), "cannot pop, array empty");
			auto out = pending_pieces[pending_pieces.size() - 1];
			pending_pieces.pop_back();
			return out;
		}

		char* pieceMemory(char piece)
		{
			ASSERT(!buffer.empty(), "cannot pop, array empty");
			return buffer.data() + (piece * CHUNK_PIECE_SIZE);
		}

		static int pieceLength(char piece)
		{
			if (piece == LAST_CHUNK_PIECE_IDX)
				return LAST_CHUNK_PIECE_SIZE;
			return CHUNK_PIECE_SIZE;
		}
	};


private:
	// net
	nd::net::Socket m_socket;
	nd::net::Address m_lobby_address;
	nd::net::Message m_big_udp_message;
	int m_port;
	bool m_is_online = false;
	int m_ticks_until_cry = 0;
	SessionID m_last_id = 1;
	int m_update_interval=4;

	int m_tick=0;
	std::string m_log;

	// player
	nd::defaultable_map_other<SessionID, ChunkMail> m_chunk_mail;
	nd::defaultable_map<std::string, SessionID> m_session_ids;
	nd::defaultable_map_other<SessionID, PlayerInfo> m_player_book;
	std::unordered_map<SessionID, nd::net::TCPTunnel> m_tunnels;

	//world
	World* m_world;

	// start, stop
	bool m_should_exit = false;
	bool m_should_restart = false;
	// if it reaches zero, server stops
	// add a little time to inform everyone about this thing
	int m_death_counter = -1;
	int m_max_players;

public:
	// getters
	bool shouldExit() const { return !m_death_counter && m_should_exit; }
	bool shouldRestart() const { return !m_death_counter && m_should_restart; }
public:

	// CONSTRUCTOR
	ServerLayer(const std::string& name, int port, nd::net::Address lobbyAddress, int maxPlayers);

	// MAIN
	void onAttach() override;
	void onDetach() override;


	bool isValidSession(SessionID id);

	void sendError(nd::net::Message& , const std::string& name, const std::string& reason);
	// ON MESSAGE RECEIVED, called when message received
	void onInvReq(nd::net::Message&);
	void onInvACK(nd::net::Message&);
	void onPlayerConnected(nd::net::Message&, PlayerInfo& info);
	void onChunkReq(nd::net::Message&);
	void onCommand(nd::net::Message&);
	void onQuit(nd::net::Message&);
	void onBlockModify(nd::net::Message&, SessionID session);
	void onMove(nd::net::Message&);
	void onInvitation(nd::net::Message&);
	void onError(nd::net::Message&);

	// UPDATES, called each tick
	void onUpdate() override;
	void updateDeathCounter();
	void updateCheckTimeout(nd::net::Message&);
	void updateSendChunks();
	void updateCryForAttention();
	void updateUDP(nd::net::Message&);
	void updateTCP(nd::net::Message&);
	void updateHopTimeout(nd::net::Message&);


	// ACTIONS
	void flushTCP();
	void disconnectClient(nd::net::Message&, SessionID id, std::string_view reason, bool sendMessage = true);
	void disconnectAll(nd::net::Message&, std::string_view reason);
	void scheduleStop(bool exit_or_restart);
	void sendClusterPong(nd::net::Address a);
	void broadcastCommand(nd::net::Message&, const std::string& sender, const std::string& cmd, SessionID except = -1);
	void sendCommand(nd::net::Message&, SessionID id, const std::string& sender, const std::string& cmd);
	void prepareMove();
	void applyMoves();
	void applyMove(EntityPlayer& player, Inputs& inputs);
	void sendMoves(nd::net::Message&);
	int playerCollisionCount();
	void broadcastTCPMessage(nd::net::Message&);
	void hop(nd::net::Message&, SessionID sessionId, const std::string& serverName);


private:
	// SPECIAL ACTIONS
	void openSocket();
	void closeSocket();
	SessionID allocateNewSessionID();
	nd::net::TCPTunnel& tun(SessionID);
};



