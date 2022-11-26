#pragma once
#include "net/net_iterator.h"
#include "core/SUtil.h"
#include "world/entity/EntityManager.h"


enum class Prot:int
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
};


constexpr const char* BANG = "[_ND_]";

struct ProtocolHeader : Serializable<ProtocolHeader>
{
	Prot action;

	bool deserialize(NetReader& reader)
	{
		auto bang = reader.readStringUntilNull(strlen(BANG));
		if (bang != BANG)
			return false;

		return reader.get(*(int*)&action);
	}

	void serialize(NetWriter& reader) const
	{
		reader.put(BANG);
		reader.put((int)action);
	}
};

struct SessionProtocol : ProtocolHeader
{
	int session_id;

	bool deserialize(NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return reader.get(session_id);
	}

	void serialize(NetWriter& writer)
	{
		ProtocolHeader::serialize(writer);
		writer.put(session_id);
	}
};

constexpr int EstablishConnectionProtocol_STRING_LENGTH = 32;

struct EstablishConnectionProtocol : SessionProtocol
{
	std::string player, server;

	bool deserialize(NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(player, EstablishConnectionProtocol_STRING_LENGTH);
		reader.get(server, EstablishConnectionProtocol_STRING_LENGTH);
		return true;
	}

	void serialize(NetWriter& writer)
	{
		SessionProtocol::serialize(writer);
		writer.put(player);
		writer.put(server);
	}
};


constexpr int CHUNK_PIECE_SIZE = 1040; //52 blocks
constexpr int LAST_CHUNK_PIECE_IDX = 19; //52 blocks
constexpr int LAST_CHUNK_PIECE_SIZE = 32 * 32 * 20 - 19 * CHUNK_PIECE_SIZE; //total bytes - 19 pieces


struct ChunkProtocol : SessionProtocol
{
	int c_x, c_y;
	char piece = -1;

	bool deserialize(NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(c_x);
		reader.get(c_y);
		reader.get(piece);

		return true;
	}

	void serialize(NetWriter& writer)
	{
		SessionProtocol::serialize(writer);
		writer.put(c_x);
		writer.put(c_y);
		if (piece != -1)
			writer.put(piece);
	}
};

//session, entity_updates[entity_id, type, physics_data] + key_press_history, block updates[x,y,block_data, time?,who]
struct WorldUpdate : SessionProtocol
{
	struct EntityUpdate : Serializable<EntityUpdate>
	{
		struct PhysicsData
		{
			glm::vec2 pos, velocity, acceleration;
		};

		EntityID id;
		EntityType type;
		PhysicsData physics;

		bool deserialize(NetReader& reader)
		{
			return
				reader.get(id) &&
				reader.get(type) &&
				reader.get(physics.pos) &&
				reader.get(physics.velocity) &&
				reader.get(physics.acceleration);
		}

		void serialize(NetWriter& reader) const
		{
			reader.put(id);
			reader.put(type);
			reader.put(physics.pos);
			reader.put(physics.velocity);
			reader.put(physics.acceleration);
		}
	};

	std::vector<EntityUpdate> entity_updates;

	bool deserialize(NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(entity_updates);
		return true;
	}

	void serialize(NetWriter& writer)
	{
		SessionProtocol::serialize(writer);
		writer.put(entity_updates);
	}
};

struct PlayerUpdate : SessionProtocol
{
	struct EntityUpdate : Serializable<EntityUpdate>
	{
		struct PhysicsData
		{
			glm::vec2 pos, velocity, acceleration;
		};

		EntityID id;
		EntityType type;
		PhysicsData physics;

		bool deserialize(NetReader& reader)
		{
			return
				reader.get(id) &&
				reader.get(type) &&
				reader.get(physics.pos) &&
				reader.get(physics.velocity) &&
				reader.get(physics.acceleration);
		}

		void serialize(NetWriter& reader) const
		{
			reader.put(id);
			reader.put(type);
			reader.put(physics.pos);
			reader.put(physics.velocity);
			reader.put(physics.acceleration);
		}
	};

	std::vector<EntityUpdate> entity_updates;

	bool deserialize(NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(entity_updates);
		return true;
	}

	void serialize(NetWriter& writer)
	{
		SessionProtocol::serialize(writer);
		writer.put(entity_updates);
	}
};
