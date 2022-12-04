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
	WorldUpdate,
	//session, entity_updates[entity_id, type, physics_data] + key_press_history, block updates[x,y,block_data, time?,who]
	PlayerUpdate,
	//session, key_press_history
	Command,
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
	"WorldUpdate	",
	"PlayerProtocol ",
	"Command		"
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

//session, entity_updates[entity_id, type, physics_data] + key_press_history, block updates[x,y,block_data, time?,who]
struct WorldUpdate : SessionProtocol
{
	struct EntityUpdate : Serializable
	{
		struct PhysicsData
		{
			glm::vec2 pos, velocity, acceleration;
		};

		EntityID id;
		EntityType type;
		PhysicsData physics;

		bool deserialize(nd::net::NetReader& reader)
		{
			return
				reader.get(id) &&
				reader.get(type) &&
				reader.get(physics.pos) &&
				reader.get(physics.velocity) &&
				reader.get(physics.acceleration);
		}

		void serialize(nd::net::NetWriter& reader) const
		{
			reader.put(id);
			reader.put(type);
			reader.put(physics.pos);
			reader.put(physics.velocity);
			reader.put(physics.acceleration);
		}
	};

	std::vector<EntityUpdate> entity_updates;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(entity_updates);
		return true;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(entity_updates);
	}
};

struct PlayerProtocol : SessionProtocol
{
	struct BlockModify : Serializable
	{
		BlockID block_id;
		bool blockOrWall;
		glm::ivec2 pos;

		bool deserialize(nd::net::NetReader& reader)
		{
			return
				reader.get(block_id) &&
				reader.get(*(int*)&blockOrWall) &&
				reader.get(pos);
		}

		void serialize(nd::net::NetWriter& writer) const
		{
			writer.put(block_id);
			writer.put(*(const int*)&blockOrWall);
			writer.put(pos);
		}
	};

	std::vector<BlockModify> block_modifies;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;

		reader.get(block_modifies);
		return true;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(block_modifies);
	}
};

struct CommandProtocol : SessionProtocol
{
	std::string message;
	CommandProtocol() { action = Prot::Command; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;

		reader.get(message);
		return true;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(message);
	}
};
