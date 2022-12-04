#pragma once
#include "ndpch.h"
#include "core/NBT.h"
#include "Block.h"

#define ND_REGISTER_BLOCK(block)\
	BlockRegistry::get().registerBlock(block);
#define ND_REGISTER_WALL(wall)\
	BlockRegistry::get().registerWall(wall);
class BlockTextureAtlas;

class BlockRegistry
{
private:
	std::unordered_map<std::string, int> m_blockIDs;
	std::unordered_map<std::string, int> m_wallIDs;
	std::unordered_map<std::string, std::pair<const ndPhys::Polygon*, int>> m_block_bounds;
	int m_currentConnectGroup = 0;
	std::unordered_map<std::string, int> m_connect_groups;
	int m_currentFlag = 0;
	std::unordered_map<std::string, int> m_block_flags;
	std::vector<Block*> m_blocks;
	std::vector<Wall*> m_walls;
	BlockRegistry() = default;


public:
	BlockRegistry(const BlockRegistry&) = delete;
	void operator=(const BlockRegistry&) = delete;
	~BlockRegistry();

	const std::vector<Block*>& getBlocks() { return m_blocks; }
	const std::vector<Wall*>& getWalls() { return m_walls; }

	static BlockRegistry& get()
	{
		static BlockRegistry s_instance;
		return s_instance;
	}

private:

	//call after entities were registered

public:

	//takes ownership
	void registerBlock(Block* block);
	//takes ownership
	void registerWall(Wall* wall);


	const Block& getBlock(BlockID block_id);

	const Wall& getWall(int wall_id);

	const Block& getBlock(const std::string& block_id) const;

	const Wall& getWall(const std::string& wall_id) const;


	BlockID getBlockID(const std::string& block_id) const { return getBlock(block_id).getID(); }
	int getWallID(const std::string& wall_id) const { return getWall(wall_id).getID(); }

	void readExternalIDList();
};
