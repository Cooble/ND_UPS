#pragma once
#include "net/net_iterator.h"
#include "core/SUtil.h"
#include "world/block/Block.h"
#include "world/entity/EntityManager.h"


enum class Prot :int
{
	// Errors
	ERR_InvalidREQ,

	// establishing connection between client and server
	// using lobby as mediator
	InvitationREQ,
	// (player)
	Invitation,
	// (player, invitation_id)
	InvitationACK,
	// (player, invitation_id)
	SessionCreated,
	//(player, session_id)

	// Migrating server
	MigrateREQ,
	// (player_data, target_server)
	MigrateWait,
	// (player)
	MigratedACK,
	// (player, server)

	// Normal Ping
	Ping,
	Pong,

	// Cluster Ping
	ClusterPing,
	ClusterPong,

	//World
	ChunkREQ,
	// session, c_x, c_y, [pieces]
	ChunkACK,
	// session, c_x,c_y, piece
	Command,
	// disconnect
	Quit,

	BlockModify,
	BlockAck,
	PlayersMoved,
	PlayerMoves
};

inline const char* ProtStrings[128] = {

	"ERR_InvalidREQ	",
	"InvitationREQ	",
	"Invitation		",
	"InvitationACK	",
	"SessionCreated	",
	"MigrateREQ		",
	"MigrateWait	",
	"MigratedACK	",
	"Ping			",
	"Pong			",
	"ClusterPing	",
	"ClusterPong	",
	"ChunkREQ		",
	"ChunkACK		",
	"Command		",
	"Quit			",
	"BlockModify	",
	"BlockAck		",
	"PlayersMoved	",
	"PlayerMoves	",

};


constexpr const char* BANG = "[_ND_]";

struct ProtocolHeader : nd::net::Serializable
{
	Prot action;

	bool deserialize(nd::net::NetReader& reader)
	{
		std::string bang;
		reader.get(bang, strlen(BANG));
		if (bang != BANG)
			return false;

		//ignore protocol name
		auto s = reader.readStringUntilNull(30);

		return reader.get(*(int*)&action);
	}

	void serialize(nd::net::NetWriter& reader) const
	{
		reader.put(BANG);
		reader.put(std::string(ProtStrings[(int)action]));
		reader.put((int)action);
	}
};

struct SessionProtocol : ProtocolHeader
{
	int session_id;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return reader.get(session_id);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(session_id);
	}
};

constexpr int EstablishConnectionProtocol_STRING_LENGTH = 32;

struct EstablishConnectionProtocol : SessionProtocol
{
	std::string player, server;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(player, EstablishConnectionProtocol_STRING_LENGTH);
		reader.get(server, EstablishConnectionProtocol_STRING_LENGTH);

		// forbid longer names
		if (player.size() > 11 && server.size() > 32)
		{
			return false;
		}
		return true;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(player);
		writer.put(server);
	}
};


constexpr int CHUNK_PIECE_SIZE = 1040; //52 blocks
constexpr int CHUNK_PIECE_COUNT = 20; //52 blocks
constexpr int LAST_CHUNK_PIECE_IDX = CHUNK_PIECE_COUNT - 1; //52 blocks
constexpr int LAST_CHUNK_PIECE_SIZE = 32 * 32 * 20 - (CHUNK_PIECE_COUNT - 1) * CHUNK_PIECE_SIZE;
//total bytes - 19 pieces


struct ChunkProtocol : SessionProtocol
{
	int c_x, c_y;
	int piece = -1;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(c_x);
		reader.get(c_y);
		reader.get(piece);

		return true;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(c_x);
		writer.put(c_y);
		writer.put(piece);
	}
};

//todo command protocol is not session protocol

struct CommandProtocol : SessionProtocol
{
	std::string message;
	CommandProtocol() { action = Prot::Command; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;

		return reader.get(message);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(message);
	}
};

struct ControlProtocol : CommandProtocol
{
	ControlProtocol() { action = Prot::Quit; }
};

struct BlockAck : ProtocolHeader
{
	int event_id;
	bool gut;

	BlockAck() { action = Prot::BlockAck; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return reader.get(event_id) && reader.get(gut);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(event_id);
		writer.put(gut);
	}
};

struct BlockModify : ProtocolHeader
{
	int block_id;
	bool blockOrWall;
	int x, y;
	int event_id;

	BlockModify() { action = Prot::BlockModify; }


	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return
			reader.get(block_id) &&
			reader.get(blockOrWall) &&
			reader.get(x) &&
			reader.get(y) &&
			reader.get(event_id);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(block_id);
		writer.put(blockOrWall);
		writer.put(x);
		writer.put(y);
		writer.put(event_id);
	}
};

struct Inputs : nd::net::Serializable
{
	bool up, down, left, right;

	bool deserialize(nd::net::NetReader& reader)
	{
		std::string s;
		if (!reader.get(s) || s.size() != 4)
			return false;

		up = s[0] == '1';
		down = s[1] == '1';
		left = s[2] == '1';
		right = s[3] == '1';

		return true;
	}

	std::string toString() const
	{
		std::string s;
		s += (char)('0' + up);
		s += (char)('0' + down);
		s += (char)('0' + left);
		s += (char)('0' + right);
		return s;
	}
	void serialize(nd::net::NetWriter& writer) const
	{
		writer.put(toString());
	}

	
};

struct PlayerMoves : SessionProtocol
{
	Inputs inputs;
	glm::vec2 pos;
	int event_id;


	PlayerMoves() { action = Prot::PlayerMoves; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;

		return
			reader.get(inputs) &&
			reader.get(pos) &&
			reader.get(event_id);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(inputs);
		writer.put(pos);
		writer.put(event_id);
	}
};

struct PlayerMoved:nd::net::Serializable
{
	// player
	std::string name;
	// inputs issued by the player in order to get to targetPos
	std::vector<Inputs> inputs;
	// final position that the player got to
	glm::vec2 targetPos;
	// final velocity that the player got to
	glm::vec2 targetVelocity;
	// latest event move id from client
	int event_id=0;

	bool deserialize(nd::net::NetReader& reader)
	{
		return
			reader.get(name) &&
			reader.get(inputs) &&
			reader.get(targetPos) &&
			reader.get(targetVelocity) &&
			reader.get(event_id);
	
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		writer.put(name);
		writer.put(inputs);
		writer.put(targetPos);
		writer.put(targetVelocity);
		writer.put(event_id);
	}
};

struct PlayersMoved:ProtocolHeader
{
	std::vector<PlayerMoved> moves;

	PlayersMoved() { action = Prot::PlayersMoved; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return reader.get(moves);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(moves);
	}

};
