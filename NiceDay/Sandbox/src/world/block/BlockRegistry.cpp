#include "ndpch.h"
#include "BlockRegistry.h"
#include "world/entity/EntityRegistry.h"
#include "basic_blocks.h"

using namespace nd;

static std::vector<std::string> allConnectGroups;

static NBT blocksNBT;




void BlockRegistry::registerBlock(Block* block)
{
	auto it = m_blockIDs.find(block->getStringID());
	ASSERT(it != m_blockIDs.end(), "Unregistered block name, write entry in blocks.ids");
	m_blocks[it->second] = block;
	block->m_id = it->second;
}

void BlockRegistry::registerWall(Wall* wall)
{
	auto it = m_wallIDs.find(wall->getStringID());
	ASSERT(it != m_wallIDs.end(), "Unregistered wall name, write entry in walls.ids");
	m_walls[it->second] = wall;
	wall->m_id = it->second;
}

const Block& BlockRegistry::getBlock(BlockID block_id)
{
	ASSERT(m_blocks.size() > block_id&&block_id>=0, "Invalid block id");
	return *m_blocks[block_id];
}

const Wall& BlockRegistry::getWall(int wall_id)
{
	ASSERT(m_walls.size() > wall_id&&wall_id >= 0, "Invalid wall id");
	return *m_walls[wall_id];
}

const Block& BlockRegistry::getBlock(const std::string& block_id) const
{
	ASSERT(m_blockIDs.find(block_id)!=m_blockIDs.end(), "Invalid block id");
	return *m_blocks[m_blockIDs.at(block_id)];
}

const Wall& BlockRegistry::getWall(const std::string& wall_id) const { return *m_walls[m_wallIDs.at(wall_id)]; }

BlockRegistry::~BlockRegistry()
{
	for (auto b : m_blocks)
		delete b;
	for (auto b : m_walls)
		delete b;
}

void BlockRegistry::readExternalIDList()
{
	{
		auto t = std::ifstream(ND_RESLOC("res/registry/blocks/blocks.ids"));
		std::string line;
		while (std::getline(t, line))
		{
			std::istringstream iss(line);
			if (line.find(' ') == std::string::npos)
				continue;
			int index;
			std::string name;
			if (!(iss >> index >> name)) { continue; } // error
			m_blockIDs[name] = index;
		}
	}
	{
		auto t = std::ifstream(ND_RESLOC("res/registry/blocks/walls.ids"));
		std::string line;
		while (std::getline(t, line))
		{
			std::istringstream iss(line);
			if (line.find(' ') == std::string::npos)
				continue;
			int index;
			std::string name;
			if (!(iss >> index >> name)) { continue; } // error
			m_wallIDs[name] = index;
		}
	}
	m_blocks.resize(m_blockIDs.size());
	m_walls.resize(m_wallIDs.size());
}