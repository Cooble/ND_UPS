#pragma once
#include <map>

#include "Prot.h"
#include "layer/Layer.h"
#include "net/net.h"
#include "world/World.h"


class Chunk;
class World;

class ServerLayer : public nd::Layer
{
private:
	typedef int SessionID;

	struct PlayerInfo
	{
		size_t lastSeen;
		nd::net::Address address;
		SessionID session_id=0;
		bool isValid=false;
		std::string name;

		bool isAlive() const;
		void updateLife();
		PlayerInfo();
	};

	char* m_buff;
	nd::net::Socket m_socket;
	nd::net::Address m_lobby_address;
	int port;
	bool m_is_online=false;
	int m_ticks_until_cry = 0;

	World* m_world;

	struct ChunkMail
	{

		ChunkID c_id;

		bool being_loaded = false;
		std::vector<char> buffer;
		std::vector<char> pending_pieces;
		bool makeVacant() { c_id = -1; }
		bool vacant() const { return c_id == -1; }
		bool pending() const { return pending_pieces.empty(); }

		void fillWithChunk(Chunk* chunk);

		void pushPendingPiece(char c)
		{
			pending_pieces.push_back(c);
		}

		char popPendingPiece()
		{
			ASSERT(!empty(), "cannot pop, array empty");
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

	nd::defaultable_map_other< SessionID, ChunkMail> m_chunk_mail;

	nd::defaultable_map<std::string, SessionID> m_session_ids;
	nd::defaultable_map_other<SessionID, PlayerInfo> m_player_book;


	void openSocket();

	void closeSocket();

	SessionID lastId=1;
	SessionID allocateNewSessionID();

public:
	ServerLayer(const std::string& name,int port,nd::net::Address lobbyAddress);

	void onAttach() override;
	void onInvReq(nd::net::Message& message);
	void onInvACK(nd::net::Message& message);
	void onChunkReq(nd::net::Message& message);
	void onUpdate() override;

	void checkTimeout();
	void onDetach() override;

	void cryForAttention();
	void sendClusterPong(nd::net::Address m);
	void sendChunks();
};
