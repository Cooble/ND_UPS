#include "ndpch.h"
#include "World.h"

#include <utility>
#include "block/Block.h"
#include "WorldIO.h"
#include "block/BlockRegistry.h"
#include "entity/EntityRegistry.h"
#include <filesystem>
#include "entity/entities.h"
#include "entity/EntityAllocator.h"

using namespace nd;

#define CHUNK_NOT_EXIST -1
constexpr int CHUNK_BUFFER_LENGTH = 400; //5*4

#define WORLD_CHECK_VALID_POS(wx,wy)\
	if ((wx) < 0 || (wy) < 0||(wx) >= getInfo().chunk_width * WORLD_CHUNK_SIZE || (wy) >= getInfo().chunk_height * WORLD_CHUNK_SIZE){\
		ND_WARN("Set block with invalid position");\
		return;\
	}

//=================WORLD======================================

void World::init()
{
	m_chunks.reserve(CHUNK_BUFFER_LENGTH);
	for (int i = 0; i < CHUNK_BUFFER_LENGTH; i++)
	{
		m_chunks.emplace_back();
	}
	memset(m_chunks.data(), 0, m_chunks.size() * sizeof(Chunk));
	ND_INFO("Made world instance");
}

World::World(std::string file_path, const WorldInfo& info)
	:
	  m_info(info),
	  m_file_path(file_path),
	  m_nbt_saver(file_path + ".entity")
{
	m_nbt_saver.initIfExist();
	init();
}

World::~World()
{
	
}

void World::onUpdate()
{
	tick();
}

static int light_tick_delay = 0;
constexpr int MAX_LIGHT_TICK_DELAY = 60;

void World::tick()
{
	static float timeAdvance = 0;
	timeAdvance += 1;
	while (timeAdvance >= 1)
	{
		m_info.time++;
		timeAdvance--;
	}
}





//====================CHUNKS=========================


Chunk* World::getChunkM(int cx, int cy)
{
	return &m_chunks[m_local_offset_header_map[half_int(cx, cy).i]];
}

int World::getChunkIndex(int cx, int cy) const
{
	return m_local_offset_header_map.at(half_int(cx, cy).i);
}

constexpr int maskUp = BIT(0);
constexpr int maskDown = BIT(1);
constexpr int maskLeft = BIT(2);
constexpr int maskRight = BIT(3);
constexpr int maskGen = BIT(4);
constexpr int maskFreshlyOnlyLoaded = BIT(5); //chunk was only loaded by thread, no gen neccessary
constexpr int maskAllSides = maskUp | maskDown | maskLeft | maskRight;
//chunk was only loaded by thread, no gen neccessary


void World::updateChunkBounds(int cx, int cy, int bitBounds)
{
	auto& c = *getChunkM(cx, cy);
	int wx = c.m_x * WORLD_CHUNK_SIZE;
	int wy = c.m_y * WORLD_CHUNK_SIZE;
	if ((bitBounds & maskUp) != 0)
		for (int x = 0; x < WORLD_CHUNK_SIZE; x++)
		{
			auto& block = c.block(x, WORLD_CHUNK_SIZE - 1);
			auto worldx = wx + x;
			auto worldy = wy + WORLD_CHUNK_SIZE - 1;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(*this, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(*this, worldx, worldy);
		}
	if ((bitBounds & maskDown) != 0)
		for (int x = 0; x < WORLD_CHUNK_SIZE; x++)
		{
			auto& block = c.block(x, 0);
			auto worldx = wx + x;
			auto worldy = wy;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(*this, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(*this, worldx, worldy);
		}
	if ((bitBounds & maskLeft) != 0)
		for (int y = 0; y < WORLD_CHUNK_SIZE; y++)
		{
			auto& block = c.block(0, y);
			auto worldx = wx;
			auto worldy = wy + y;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(*this, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(*this, worldx, worldy);
		}
	if ((bitBounds & maskRight) != 0)
		for (int y = 0; y < WORLD_CHUNK_SIZE; y++)
		{
			auto& block = c.block(WORLD_CHUNK_SIZE - 1, y);
			auto worldx = wx + WORLD_CHUNK_SIZE - 1;
			auto worldy = wy + y;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(*this, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(*this, worldx, worldy);
		}
}
//=========================BLOCKS==========================

const BlockStruct* World::getBlock(int x, int y) const
{
	if (!isValidLocation(x, y))
	{
		//ND_WARN("INvalid world location");
		return nullptr;
	}
	int index = getChunkIndex(x >> WORLD_CHUNK_BIT_SIZE, y >> WORLD_CHUNK_BIT_SIZE);
	if (index != -1)
		return const_cast<BlockStruct*>(&m_chunks[index].block(x & (WORLD_CHUNK_SIZE - 1),
		                                                       y & (WORLD_CHUNK_SIZE - 1)));
	return nullptr;
}

const Block& World::getBlockInstance(int x, int y) const
{
	auto t = getBlock(x, y);
	if (t == nullptr)
		return BlockRegistry::get().getBlock(0);
	return BlockRegistry::get().getBlock(t->block_id);
}

const BlockStruct* World::getBlockOrAir(int x, int y) const
{
	auto t = getBlock(x, y);
	static BlockStruct b = 0;
	return t == nullptr ? &b : t;
}

BlockStruct* World::getBlockM(int x, int y)
{
	return const_cast<BlockStruct*>(getBlock(x, y));
}

void World::flushBlockSet()
{
	m_edit_buffer_enable = false;
	while (!m_edit_buffer.empty())
	{
		auto pair = m_edit_buffer.front();
		m_edit_buffer.pop();
		BlockRegistry::get().getBlock(getBlockM(pair.first, pair.second)->block_id).onNeighborBlockChange(
			*this, pair.first, pair.second);
		onBlocksChange(pair.first, pair.second);
	}
}

void World::setBlockWithNotify(int x, int y, BlockStruct& newBlock)
{
	auto blok = getBlockM(x, y);
	if (blok == nullptr)
		return;

	auto& regOld = BlockRegistry::get().getBlock(blok->block_id);
	auto& regNew = BlockRegistry::get().getBlock(newBlock.block_id);


	memcpy(newBlock.wall_id, blok->wall_id, sizeof(newBlock.wall_id));
	memcpy(newBlock.wall_corner, blok->wall_corner, sizeof(newBlock.wall_corner));

	regOld.onBlockDestroyed(*this, nullptr, x, y, *blok);

	*blok = newBlock;

	if (!m_edit_buffer_enable)
	{
		regNew.onBlockPlaced(*this, nullptr, x, y, *blok);
		regNew.onNeighborBlockChange(*this, x, y);
		onBlocksChange(x, y);
	}
	else
		m_edit_buffer.emplace(x, y);

	getChunkM(x >> WORLD_CHUNK_BIT_SIZE, y >> WORLD_CHUNK_BIT_SIZE)->markDirty(true);
}

void World::setBlock(int x, int y, BlockStruct& newBlock)
{
	auto blok = getBlockM(x, y);
	if (blok == nullptr)
		return;

	auto& regOld = BlockRegistry::get().getBlock(blok->block_id);
	auto& regNew = BlockRegistry::get().getBlock(newBlock.block_id);


	memcpy(newBlock.wall_id, blok->wall_id, sizeof(newBlock.wall_id));
	memcpy(newBlock.wall_corner, blok->wall_corner, sizeof(newBlock.wall_corner));

	*blok = newBlock;
}

constexpr int MAX_BLOCK_UPDATE_DEPTH = 20;

void World::onBlocksChange(int x, int y, int deep)
{
	++deep;
	if (deep >= MAX_BLOCK_UPDATE_DEPTH)
	{
		ND_WARN("Block update too deep! protecting stack");
		return;
	}

	auto p = getBlockM(x, y + 1);
	if (p && BlockRegistry::get().getBlock(p->block_id).onNeighborBlockChange(*this, x, y + 1))
	{
		getChunkM(Chunk::getChunkIDFromWorldPos(x, y + 1))->markDirty(true);
		onBlocksChange(x, y + 1, deep);
	}
	p = getBlockM(x, y - 1);
	if (p && BlockRegistry::get().getBlock(p->block_id).onNeighborBlockChange(*this, x, y - 1))
	{
		getChunkM(Chunk::getChunkIDFromWorldPos(x, y - 1))->markDirty(true);
		onBlocksChange(x, y - 1, deep);
	}
	p = getBlockM(x + 1, y);
	if (p && BlockRegistry::get().getBlock(p->block_id).onNeighborBlockChange(*this, x + 1, y))
	{
		getChunkM(Chunk::getChunkIDFromWorldPos(x + 1, y))->markDirty(true);
		onBlocksChange(x + 1, y, deep);
	}
	p = getBlockM(x - 1, y);
	if (p && BlockRegistry::get().getBlock(p->block_id).onNeighborBlockChange(*this, x - 1, y))
	{
		getChunkM(Chunk::getChunkIDFromWorldPos(x - 1, y))->markDirty(true);
		onBlocksChange(x - 1, y, deep);
	}
}

//=========================WALLS===========================

void World::setWallWithNotify(int x, int y, int wall_id)
{
	auto blok = getBlockM(x, y);
	if (blok == nullptr)
		return;

	auto& regOld = BlockRegistry::get().getBlock(blok->block_id);

	if (!blok->isWallFullyOccupied() && wall_id == 0) //cannot erase other blocks parts on this shared block
		return;
	blok->setWall(wall_id);
	onWallsChange(x, y, *blok);



	getChunkM(x >> WORLD_CHUNK_BIT_SIZE, y >> WORLD_CHUNK_BIT_SIZE)->markDirty(true);
}

void World::onWallsChange(int xx, int yy, BlockStruct& blok)
{
	BlockRegistry::get().getWall(blok.wallID()).onNeighbourWallChange(*this, xx, yy);
	/*for (int x = -1; x < 2; ++x)
	{
		for (int y = -1; y < 2; ++y)
		{
			if (x == 0 && y == 0)
				continue;
			if (isValidBlock(x + xx, y + yy)) {
				BlockStruct& b = *getBlock(x + xx, y + yy);
				if (b.isWallFullyOccupied())//i cannot call this method on some foreign wall pieces
					BlockRegistry::get().getWall(b.wallID()).onNeighbourWallChange(*this, x + xx, y + yy);
			}
		}
	}*/
	for (int x = -2; x < 3; ++x)
	{
		for (int y = -2; y < 3; ++y)
		{
			/*if (x >=-1&&x<2)
				continue;
			if (y >= -1 && y < 2)
				continue;*/
			if (x == 0 && y == 0)
				continue;
			if (isBlockValid(x + xx, y + yy))
			{
				auto& b = *getBlockM(x + xx, y + yy);
				if (b.isWallFullyOccupied())
				{
					//i cannot call this method on some foreign wall pieces
					BlockRegistry::get().getWall(b.wallID()).onNeighbourWallChange(*this, x + xx, y + yy);
					auto& c = *getChunkM(toChunkCoord(x + xx), toChunkCoord(y + yy));
					c.markDirty(true); //mesh needs to be updated
				}
			}
		}
	}
}

//===================ENTITIES============================

//======================WORLD IO=========================
