#pragma once
#include "ndpch.h"
#include "WorldIO.h"
#include "gen/WorldGen.h"
#include "block/Block.h"
#include "entity/WorldEntity.h"
#include "world/WorldTime.h"
#include "core/NBT.h"


constexpr int DYNAMIC_ID_ENTITY_MANAGER = std::numeric_limits<int>::max() - 0;
constexpr int DYNAMIC_ID_WORLD_NBT = std::numeric_limits<int>::max() - 1;

const int WORLD_CHUNK_BIT_SIZE = 5;
const int WORLD_CHUNK_SIZE = 32;
const int WORLD_CHUNK_AREA = WORLD_CHUNK_SIZE * WORLD_CHUNK_SIZE;

static const int BITS_FOR_CHUNK_LOC = 16;

constexpr int CHUNK_DIRTY_FLAG = BIT(1); //in next update chunk graphics will be reloaded into chunkrenderer
constexpr int CHUNK_LOCKED_FLAG = BIT(2); //dont unload this chunk its being worked on by main thread

typedef int ChunkID;
class World;

class Chunk
{
private:
	int m_x, m_y;

	//posxy

	BlockStruct m_blocks[WORLD_CHUNK_AREA];
	uint32_t m_flags = 0;


public:
	friend class World;
	friend class WorldIO::Session;
	friend class WorldGen;

	long long last_save_time;
	int getCX() const { return m_x; }
	int getCY() const { return m_y; }
	ChunkID chunkID() const { return half_int(m_x, m_y); }

	// cannot unload locked chunk
	bool isLocked() const
	{
		return (m_flags & CHUNK_LOCKED_FLAG) != 0;
	}

	bool isDirty() const { return m_flags & CHUNK_DIRTY_FLAG; }
	bool isGenerated() const { return last_save_time != 0; } //worldgen has generated it
	void lock(bool lock)
	{
		if (lock) m_flags |= CHUNK_LOCKED_FLAG;
		else m_flags &= ~CHUNK_LOCKED_FLAG;
	}

	BlockStruct& block(int x, int y)
	{
		ASSERT(x >= 0 && x < WORLD_CHUNK_SIZE&&y >= 0 && y < WORLD_CHUNK_SIZE, "Invalid chunk coords!");
		return m_blocks[y << WORLD_CHUNK_BIT_SIZE | x];
	}

	const BlockStruct& block(int x, int y) const { return m_blocks[y << WORLD_CHUNK_BIT_SIZE | x]; }


	void markDirty(bool dirty=true)
	{
		if (dirty) m_flags |= CHUNK_DIRTY_FLAG;
		else m_flags &= ~CHUNK_DIRTY_FLAG;
	}


	static int getChunkIDFromWorldPos(int wx, int wy)
	{
		return half_int(wx >> WORLD_CHUNK_BIT_SIZE, wy >> WORLD_CHUNK_BIT_SIZE);
	}
};

struct WorldInfo
{
	long seed;
	int chunk_width, chunk_height;
	int terrain_level;
	long long time = 0;
	char name[100]{};
	EntityID player_id;
	ChunkID playerChunk;
};


class World
{
	friend class WorldIO::Session;
	friend class WorldGen;
public:
	enum ChunkState
	{
		BEING_LOADED,
		BEING_GENERATED,
		BEING_BOUNDED,
		GENERATED,
		BEING_UNLOADED,
		UNLOADED
	};


	static int toChunkCoord(float x) { return toChunkCoord((int)x); }
	static int toChunkCoord(int x) { return x >> WORLD_CHUNK_BIT_SIZE; }

private:
	WorldGen m_gen;
	std::vector<Chunk> m_chunks;
	nd::NBT m_world_nbt;

	WorldInfo m_info;
	std::string m_file_path;

	//todo problem used by gen and main thread
	std::unordered_map<int, int> m_local_offset_header_map; //chunkID,offsetInBuffer
	BlockStruct m_air_block = 0;

	EntityManager m_entity_manager;

	// needs to be created outside world and set
	nd::DynamicSaver m_nbt_saver;

	std::vector<EntityID> m_entity_array;
	std::unordered_map<int64_t, EntityID> m_tile_entity_map;
	std::vector<EntityID> m_entity_array_buff;
	std::unordered_map<int64_t, EntityID> m_tile_entity_array_buff;


	bool m_edit_buffer_enable = false;
	std::queue<std::pair<int, int>> m_edit_buffer; //location x,y for editted blocks

private:
	void init();
	void onBlocksChange(int x, int y, int deep = 0);
	void onWallsChange(int xx, int yy, BlockStruct& blok);
	void updateChunkBounds(int cx, int cy, int bitBounds);

	bool isValidLocation(float wx, float wy) const
	{
		return !((wx) < 0 || (wy) < 0 || (wx) >= getInfo().chunk_width * WORLD_CHUNK_SIZE || (wy) >= getInfo().chunk_height * WORLD_CHUNK_SIZE);
	}

public:
	World(std::string file_path, const WorldInfo& info);
	~World();

	void onUpdate();
	void tick();

	bool isBlockValid(int x, int y) const
	{
		return x >= 0 && y >= 0 && x < getInfo().chunk_width * WORLD_CHUNK_SIZE && y < getInfo().chunk_height *
			WORLD_CHUNK_SIZE;
	}

	bool isChunkValid(int x, int y) const
	{
		return isChunkValid(half_int(x, y));
	}

	bool isChunkValid(half_int chunkid) const
	{
		return chunkid.x >= 0 && chunkid.x < getInfo().chunk_width && chunkid.y >= 0 && chunkid.y < getInfo().
			chunk_height;
	}

	int getChunkSaveOffset(int id) const
	{
		return getChunkSaveOffset(half_int::X(id),half_int::Y(id));
	}

	
	int getChunkSaveOffset(int cx,int cy) const
	{
		return cx + cy*m_info.chunk_width;
	};
	//==============CHUNK METHODS========================================================================

	//return nullptr if chunk is not loaded or invalid coords 
	//(won't cause chunk load)
	Chunk* getChunkM(int cx, int cy);
	int getChunkIndex(int cx, int cy) const;
	Chunk* getChunkM(half_int chunkID) { return getChunkM(chunkID.x, chunkID.y); }
	
	//==============BLOCK METHODS=======================================================================

	//return nullptr if block is not loaded or invalid coords 
	//(won't cause chunk load)
	const BlockStruct* getBlock(int x, int y) const;
	const Block& getBlockInstance(int x, int y) const;

	// never returns nullptr
	const BlockStruct* getBlockOrAir(int x, int y) const;

	//return nullptr if invalid coords 
	BlockStruct* getBlockM(int x, int y);


	//return is block at coords is air or true if outside the map
	bool isAir(int x, int y)
	{
		auto b = getBlockM(x, y);
		return b == nullptr || b->isAir();
	}
	//return is wall at coords is air or shared or true if outside the map
	bool isWallFree(int x, int y)
	{
		auto b = getBlockM(x, y);
		return b == nullptr || b->isWallFree();
	}

	//automatically calls chunk.markdirty() to update graphics and call onNeighborBlockChange()
	void setBlockWithNotify(int x, int y, BlockStruct& block);

	// just changes block value of blockstruct (no notification)
	void setBlock(int x, int y, BlockStruct& block);
	void setBlock(int x, int y, int blockid) { setBlock(x, y, BlockStruct(blockid)); }
	void setBlockWithNotify(int x, int y, int blockid) { setBlockWithNotify(x, y, BlockStruct(blockid)); }


	//automatically calls chunk.markdirty() to update graphics and call onNeighbourWallChange()
	void setWallWithNotify(int x, int y, int wall_id);

	// ====buffering block changes====
	//opens block set session
	//you can call many setblock()
	//then you need to flush changes flushBlockSet()
	void beginBlockSet()
	{
		ASSERT(!m_edit_buffer_enable, "Called beginedit without calling flushEdit() beforehand");
		m_edit_buffer_enable = true;
	}

	//update all setblock block states
	void flushBlockSet();

	//==========INFO==================================================

	std::unordered_map<int, int>& getMap() { return m_local_offset_header_map; }
	long long getWorldTicks() const { return m_info.time; }
	WorldTime getWorldTime() const { return WorldTime(m_info.time); }
	std::string getName() const { return m_info.name; }
	const WorldInfo& getInfo() const { return m_info; };
	const std::string& getFilePath() const { return m_file_path; }
	auto& modifyInfo() { return m_info; }
	nd::NBT& getWorldNBT() { return m_world_nbt; }
	WorldGen& getWorldGen() { return m_gen; }

	//================ENTITY========================================

	auto& getNBTSaver() { return m_nbt_saver; }
	EntityManager& getEntityManager() { return m_entity_manager; }


	//==============SERIALIZATION============================
public:
};

