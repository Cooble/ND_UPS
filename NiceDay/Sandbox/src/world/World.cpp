﻿#include "ndpch.h"
#include "World.h"

#include <block_ids.h>
#include <complex>

#include "block/Block.h"
#include "WorldIO.h"
#include "block/BlockRegistry.h"
#include "entity/EntityRegistry.h"
#include <filesystem>

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
	m_chunks.reserve(m_info.chunk_width * m_info.chunk_height);

	for (int i = 0; i < m_info.chunk_width; ++i)
		for (int j = 0; j < m_info.chunk_height; ++j)
		{
			auto c = new Chunk();
			c->m_x = i;
			c->m_y = j;
			m_chunks.emplace(half_int(i, j), c);
		}
	ND_INFO("Made world instance");
}

World::World(std::string file_path, const WorldInfo& info)
	:
	m_info(info),
	m_file_path(file_path),
	m_nbt_saver(file_path + ".world")
{
	m_nbt_saver.init();
	init();
}

World::~World()
{
	for (auto& c : m_chunks)
		delete c.second;
}


void World::onUpdate()
{
	tick();
	/*if (!taskActive && !loadQueue.empty())
	{
		taskActive = true;
		genChunks(loadQueue.front());
		loadQueue.pop();
	}*/
}

void World::tick()
{
}


//====================CHUNKS=========================

static int mandelbrot(float x, float y, int steps)
{
	std::complex<float> C(x, y);
	std::complex<float> Z(0.f, 0.f);
	for (int i = 0; i < steps; ++i)
	{
		Z = Z * Z + C;
		if (Z.imag() * Z.imag() + Z.real() * Z.real() > 1000000)
			return i;
	}
	return -1;
}

void World::genMandel()
{
	float scale = 1.f / 16;
	float offsetX = -m_info.chunk_width * WORLD_CHUNK_SIZE * scale / 2.f;
	float offsetY = -m_info.chunk_height * WORLD_CHUNK_SIZE * scale / 2.f;

	BlockID ste[5] = {BLOCK_STONE, BLOCK_GOLD, BLOCK_SNOW, BLOCK_DIRT, BLOCK_SNOW_BRICK};
	for (int cx = 0; cx < m_info.chunk_width; ++cx)
	{
		for (int cy = 0; cy < m_info.chunk_height; ++cy)
		{
			for (int x = 0; x < WORLD_CHUNK_SIZE; ++x)
			{
				for (int y = 0; y < WORLD_CHUNK_SIZE; ++y)
				{
					auto totalX = x + WORLD_CHUNK_SIZE * cx;
					auto totalY = y + WORLD_CHUNK_SIZE * cy;

					auto FX = totalX * scale + offsetX;
					auto FY = totalY * scale + offsetY;

					int steps = mandelbrot(FX, FY, 20);
					BlockStruct b = {};
					if (steps == -1)
					{
						b.block_id = BLOCK_STONE;
					}
					else
					{
						if (steps >= sizeof(ste) / sizeof(int))
							b.block_id = BLOCK_AIR;
						else
							b.block_id = ste[steps];
					}
					setBlockWithNotify(totalX, totalY, b);
					setWallWithNotify(totalX, totalY, WALL_AIR);
				}
			}
		}
	}
}

void World::genWorld()
{
	genMandel();
	return;
	BlockID ids[5]{BLOCK_AIR, BLOCK_DIRT, BLOCK_SNOW, BLOCK_SNOW_BRICK, BLOCK_STONE};
	BlockID walls[4]{WALL_AIR, WALL_DIRT, WALL_SNOW, WALL_STONE};
	int i = 0;
	for (auto& chunk : m_chunks)
	{
		auto chunkOffsetX = chunk.second->getCX() * WORLD_CHUNK_SIZE;
		auto chunkOffsetY = chunk.second->getCY() * WORLD_CHUNK_SIZE;
		for (int x = 0; x < WORLD_CHUNK_SIZE; ++x)
		{
			for (int y = 0; y < WORLD_CHUNK_SIZE; ++y)
			{
				setBlockWithNotify(x + chunkOffsetX, y + chunkOffsetY, BlockStruct(ids[(i++) % 5]));
				setWallWithNotify(x + chunkOffsetX, y + chunkOffsetY, walls[i / 5 % 4]);
			}
		}
	}
}

const Chunk* World::getChunk(int cx, int cy) const
{
	auto it = m_chunks.find(half_int(cx, cy));
	if (it == m_chunks.end())
		return nullptr;
	return it->second;
}

Chunk* World::getChunkM(int cx, int cy)
{
	return const_cast<Chunk*>(getChunk(cx, cy));
}


void World::saveWorld()
{
	ND_TRACE("Saving world to {}", m_file_path);
	m_nbt_saver.beginSession();
	for(auto& c:m_chunks)
	{
		m_nbt_saver.setWriteChunkID(c.second->chunkID());
		m_nbt_saver.write((const char*)c.second, sizeof(Chunk));
	}
	m_nbt_saver.setWriteChunkID(DYNAMIC_ID_WORLD_INFO);
	m_nbt_saver.write(m_info);
	m_nbt_saver.flushWrite();
	m_nbt_saver.endSession();
	ND_TRACE("Saving world finished");
}

bool World::loadWorld()
{
	if (!std::filesystem::exists(m_file_path+".world"))
		return false;
	ND_TRACE("Loading world from file {}",m_file_path);
	m_nbt_saver.beginSession();
	for (auto& c : m_chunks)
	{
		if(!m_nbt_saver.setReadChunkID(c.second->chunkID()))
		{
			ND_ERROR("World file corrupted");
			return false;
		}
		m_nbt_saver.read((char*)c.second, sizeof(Chunk));

	}
	if(!m_nbt_saver.setReadChunkID(DYNAMIC_ID_WORLD_INFO))
	{
		ND_ERROR("World file corrupted");
		return false;
	}
	m_nbt_saver.read(m_info);
	m_nbt_saver.endSession();
	return true;
}

constexpr int maskUp = BIT(0);
constexpr int maskDown = BIT(1);
constexpr int maskLeft = BIT(2);
constexpr int maskRight = BIT(3);
constexpr int maskGen = BIT(4);
constexpr int maskFreshlyOnlyLoaded = BIT(5); //chunk was only loaded by thread, no gen neccessary
constexpr int maskAllSides = maskUp | maskDown | maskLeft | maskRight;
//chunk was only loaded by thread, no gen neccessary

static defaultable_map<ChunkID, bool, false> CHUNK_MAP;

// calls genchunks2 with promise:
//		all chunks in toLoadChunks	are valid
//		have their header and space
//		have GENERATED or BEING_LOADED state
//		maskGen which should be generated
//		maskFreshlyOnlyLoaded should load their entities


void World::updateChunkBounds(BlockAccess& world, int cx, int cy, int bitBounds)
{
	auto& c = *world.getChunkM(cx, cy);
	int wx = c.m_x * WORLD_CHUNK_SIZE;
	int wy = c.m_y * WORLD_CHUNK_SIZE;
	if ((bitBounds & maskUp) != 0)
		for (int x = 0; x < WORLD_CHUNK_SIZE; x++)
		{
			auto& block = c.block(x, WORLD_CHUNK_SIZE - 1);
			auto worldx = wx + x;
			auto worldy = wy + WORLD_CHUNK_SIZE - 1;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(world, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(world, worldx, worldy);
		}
	if ((bitBounds & maskDown) != 0)
		for (int x = 0; x < WORLD_CHUNK_SIZE; x++)
		{
			auto& block = c.block(x, 0);
			auto worldx = wx + x;
			auto worldy = wy;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(world, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(world, worldx, worldy);
		}
	if ((bitBounds & maskLeft) != 0)
		for (int y = 0; y < WORLD_CHUNK_SIZE; y++)
		{
			auto& block = c.block(0, y);
			auto worldx = wx;
			auto worldy = wy + y;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(world, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(world, worldx, worldy);
		}
	if ((bitBounds & maskRight) != 0)
		for (int y = 0; y < WORLD_CHUNK_SIZE; y++)
		{
			auto& block = c.block(WORLD_CHUNK_SIZE - 1, y);
			auto worldx = wx + WORLD_CHUNK_SIZE - 1;
			auto worldy = wy + y;
			BlockRegistry::get().getBlock(block.block_id).onNeighborBlockChange(world, worldx, worldy);
			if (block.isWallFullyOccupied())
				BlockRegistry::get().getWall(block.wallID()).onNeighbourWallChange(world, worldx, worldy);
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
	auto chunkPtr = getChunk(x >> WORLD_CHUNK_BIT_SIZE, y >> WORLD_CHUNK_BIT_SIZE);
	if (chunkPtr)
		return const_cast<BlockStruct*>(&chunkPtr->block(x & (WORLD_CHUNK_SIZE - 1),
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


	regNew.onBlockPlaced(*this, nullptr, x, y, *blok);
	regNew.onNeighborBlockChange(*this, x, y);
	onBlocksChange(x, y);


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


//=========================PARTICLES=====================
