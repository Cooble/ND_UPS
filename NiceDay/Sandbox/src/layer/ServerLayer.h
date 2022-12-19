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

	// everything about player, sorta
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

	// contains one chunk worth of info
	// used to send pieces of this chunk to client with udp
	struct ChunkMail
	{
		ChunkID c_id = -1;
		// the big data
		std::vector<char> buffer;
		// pieces to send to client
		std::vector<char> pending_pieces;
		void makeVacant() { c_id = -1; }
		bool vacant() const { return c_id == -1; }
		bool pending() const { return !pending_pieces.empty(); }

		// fills data with chunk
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
		// ptr when particular piece starts
		char* pieceMemory(char piece)
		{
			ASSERT(!buffer.empty(), "cannot pop, array empty");
			return buffer.data() + (piece * CHUNK_PIECE_SIZE);
		}
		// length of particular piece
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
	int m_port;

	nd::net::Address m_lobby_address;
	// big universal buffer
	nd::net::Message m_big_udp_message;
	bool m_is_online = false;
	// number of ticks until next ping
	int m_ticks_until_cry = 0;
	// last session id used
	SessionID m_last_id = 1;

	// number of ticks between move update sending
	int m_update_interval=4;

	// player
	// session id -> chunk
	nd::defaultable_map_other<SessionID, ChunkMail> m_chunk_mail;
	// name ->session id
	nd::defaultable_map<std::string, SessionID> m_session_ids;
	// session id -> player
	nd::defaultable_map_other<SessionID, PlayerInfo> m_player_book;
	// session id -> tcp tunnel
	std::unordered_map<SessionID, nd::net::TCPTunnel> m_tunnels;

	//world
	World* m_world=nullptr;

	// start, stop
	bool m_should_exit = false;
	bool m_should_restart = false;
	// if it reaches zero, server stops
	// add a little time to inform everyone about this thing
	int m_death_counter = -1;
	//limit of players
	int m_max_players;

	// stats
	int m_received_packets=0;
	int m_received_tcp_messages=0;
	int m_sent_packets=0;
	int m_sent_tcp_messages=0;


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


	// ======  ON MESSAGE RECEIVED, called when message received ========

	// player wants to connect, send him an invitation
	void onInvReq(nd::net::Message&);
	// player accepted invitation, validate him and send session created
	void onInvACK(nd::net::Message&);
	// player connected, create tcp tunnel, create PlayerEntity etc.
	void onPlayerConnected(nd::net::Message&, PlayerInfo& info);
	// client wants chunk
	void onChunkReq(nd::net::Message&);
	// client issues a chat command using tcp
	void onCommand(nd::net::Message&);
	// client has disconnected from us :(
	void onQuit(nd::net::Message&);
	// client tries to break block and waits for acknowledgment from server
	void onBlockModify(nd::net::Message&, SessionID session);
	// client sends move data, every tick
	void onMove(nd::net::Message&);
	// different server accepted move of our player to it, redirect to client
	void onInvitation(nd::net::Message&);
	// received error from lobby, lets ignore it :), or print it to console for all I care
	void onError(nd::net::Message&);

	// ========= UPDATES CALLED EACH TICK =================
	void onUpdate() override;
	// check if should die
	void updateDeathCounter();
	// kill old players
	void updateCheckTimeout(nd::net::Message&);
	// send chunk pieces to all yearning clients
	void updateSendChunks();
	// no lobby server connected yet, send scream to lobby
	void updateCryForAttention();
	void updateUDP(nd::net::Message&);
	void updateTCP(nd::net::Message&);
	// check if our request to move player to another server timed out
	void updateHopTimeout(nd::net::Message&);


	// ========= ACTIONS =================

	// flush all data in tcp buffers
	void flushTCP();
	// disconnect player, optionally sends him a reason why
	void disconnectClient(nd::net::Message&, SessionID id, std::string_view reason, bool sendMessage = true);
	// disconnects and saves all players, also sends reason using tcp
	void disconnectAll(nd::net::Message&, std::string_view reason);
	// sever will shutdown or restart
	void scheduleStop(bool exit_or_restart);
	// send pong back to lobby
	void sendClusterPong(nd::net::Address a);
	// tcp, send to all except
	void broadcastCommand(nd::net::Message&, const std::string& sender, const std::string& cmd, SessionID except = -1);
	// tcp
	void sendCommand(nd::net::Message&, SessionID id, const std::string& sender, const std::string& cmd);
	// move players based on their velocities
	void applyMoves();
	// apply velocity to player based on inputs
	void applyMove(EntityPlayer& player, Inputs& inputs);
	// send update of movements for all players, every N ticks
	void sendMoves(nd::net::Message&);

	// number of players colliding with blocks 
	int playerCollisionCount();
	//send tcp message to all valid players
	void broadcastTCPMessage(nd::net::Message&);
	// asks lobby server to connect player to certain server, udp
	void hop(nd::net::Message&, SessionID sessionId, const std::string& serverName);
	// sends udp error
	void sendError(nd::net::Message&, const std::string& playerName, const std::string& reason);

	std::string buildStats();


private:
	// SPECIAL ACTIONS
	bool openSocket();
	void closeSocket();
	SessionID allocateNewSessionID();
	nd::net::TCPTunnel& tun(SessionID);
};



