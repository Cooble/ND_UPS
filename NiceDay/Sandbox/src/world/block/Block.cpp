#include "ndpch.h"

#include "world/World.h"
#include "world/BlockAccess.h"
#include "Block.h"
#include "BlockRegistry.h"
#include "block_datas.h"
#include "world/entity/EntityRegistry.h"
#include "world/entity/EntityAllocator.h"

using namespace nd;

Block::Block(std::string id)
	: m_id(-1),
	  m_string_id(std::move(id)),
	  m_collision_box(BLOCK_BOUNDS_DEFAULT),
	  m_collision_box_size(3),
	  m_block_connect_group(0)
{
	setFlag(BLOCK_FLAG_HAS_ITEM_VERSION, true);
	setFlag(BLOCK_FLAG_SOLID, true);
}

Phys::Vecti Block::getTileEntityCoords(int x, int y, const BlockStruct& b) const { return Phys::Vecti(x, y); }

const Phys::Polygon& Block::getCollisionBox(int x, int y, const BlockStruct& b) const
{
	auto i = m_collision_box_size >= 3 ? 1 : 0;
	switch (BLOCK_STATE_PURE_MASK & b.block_corner)
	{
	case BLOCK_STATE_CORNER_UP_LEFT:
		return m_collision_box[1 * i];
	case BLOCK_STATE_CORNER_UP_RIGHT:
		return m_collision_box[2 * i];
	case BLOCK_STATE_CORNER_DOWN_LEFT:
		return m_collision_box[3 * i];
	case BLOCK_STATE_CORNER_DOWN_RIGHT:
		return m_collision_box[4 * i];
	case BLOCK_STATE_LINE_UP:
		return m_collision_box[5 * i * (bool)(b.block_corner & BLOCK_STATE_HAMMER)]; //needs to be hammer to work
	case BLOCK_STATE_LINE_DOWN:
		return m_collision_box[6 * i * (bool)(b.block_corner & BLOCK_STATE_HAMMER)]; //needs to be hammer to work
	default:
		return m_collision_box[0];
	}
}

bool Block::hasCollisionBox() const { return m_collision_box_size != 0; }






bool Block::isInGroup(BlockAccess& w, int x, int y, int group) const
{
	auto b = w.getBlockM(x, y);
	if (b)
	{
		auto& bl = BlockRegistry::get().getBlock(b->block_id);
		return bl.isInConnectGroup(group) || bl.getID() == m_id;
	}
	return false;
}

bool Block::isInGroup(BlockID blockID, int group) const
{
	auto& bl = BlockRegistry::get().getBlock(blockID);
	return bl.isInConnectGroup(group) || bl.getID() == m_id;
}

bool Block::onNeighborBlockChange(BlockAccess& world, int x, int y) const
{
	int mask = 0;
	mask |= (!isInGroup(world, x, y + 1, m_block_connect_group) & 1) << 0;
	mask |= (!isInGroup(world, x - 1, y, m_block_connect_group) & 1) << 1;
	mask |= (!isInGroup(world, x, y - 1, m_block_connect_group) & 1) << 2;
	mask |= (!isInGroup(world, x + 1, y, m_block_connect_group) & 1) << 3;


	BlockStruct& block = *world.getBlockM(x, y);
	int lastCorner = block.block_corner;
	block.block_corner = mask | (lastCorner & ~BLOCK_STATE_PURE_MASK);
	return lastCorner != block.block_corner; //we have a change (or not)
}

void Block::onBlockPlaced(World& w, WorldEntity* e, int x, int y, BlockStruct& b) const
{
	
}

void Block::onBlockDestroyed(World& w, WorldEntity* e, int x, int y, BlockStruct& b) const
{
	
}

void Block::onBlockClicked(World& w, WorldEntity* e, int x, int y, BlockStruct& b) const
{
	
}

bool Block::canBePlaced(World& w, int x, int y) const
{
	// can be hanged only on wall
	if (needsWall() && w.getBlock(x, y)->isWallFree())
		return false;

	// block is already occupied with another block
	if (!w.getBlockInstance(x, y).isReplaceable())
		return false;

	// can be put only on ground
	if (cannotFloat() && !w.getBlockInstance(x, y - 1).isSolid())
		return false;

	return true;
}





//WALL======================================================
Wall::Wall(std::string id)
	: m_string_id(std::move(id))
{
	setFlag(BLOCK_FLAG_HAS_ITEM_VERSION, true);
	setFlag(BLOCK_FLAG_SOLID, true);
}

Wall::~Wall() = default;



static bool isWallFree(BlockAccess& w, int x, int y)
{
	auto b = w.getBlockM(x, y);
	return b ? b->isWallFree() : false; //if we dont know we will assume is occupied
}

void Wall::onNeighbourWallChange(BlockAccess& w, int x, int y) const
{
	BlockStruct& block = *w.getBlockM(x, y);
	auto p = w.getBlockM(x, y);
	ASSERT(p, "fuk");
	//left
	if (isWallFree(w, x - 1, y))
	{
		auto& b = *w.getBlockM(x - 1, y);
		b.wall_id[1] = m_id;
		b.wall_id[3] = m_id;
		b.wall_corner[1] = BLOCK_STATE_LINE_LEFT;
		b.wall_corner[3] = BLOCK_STATE_LINE_LEFT;
	}
	//right
	if (isWallFree(w, x + 1, y))
	{
		auto& b = *w.getBlockM(x + 1, y);
		b.wall_id[0] = m_id;
		b.wall_id[2] = m_id;
		b.wall_corner[0] = BLOCK_STATE_LINE_RIGHT;
		b.wall_corner[2] = BLOCK_STATE_LINE_RIGHT;
	}
	//up
	if (isWallFree(w, x, y + 1))
	{
		auto& b = *w.getBlockM(x, y + 1);
		b.wall_id[0] = m_id;
		b.wall_id[1] = m_id;
		b.wall_corner[0] = BLOCK_STATE_LINE_UP;
		b.wall_corner[1] = BLOCK_STATE_LINE_UP;
	}
	//down
	if (isWallFree(w, x, y - 1))
	{
		auto& b = *w.getBlockM(x, y - 1);
		b.wall_id[2] = m_id;
		b.wall_id[3] = m_id;
		b.wall_corner[2] = BLOCK_STATE_LINE_DOWN;
		b.wall_corner[3] = BLOCK_STATE_LINE_DOWN;
	}
	//corner left up

	if (isWallFree(w, x - 1, y + 1)
		&& isWallFree(w, x - 1, y)
		&& isWallFree(w, x, y + 1))
	{
		auto& b = *w.getBlockM(x - 1, y + 1);
		b.wall_id[1] = m_id;
		b.wall_corner[1] = BLOCK_STATE_CORNER_UP_LEFT;
	}

	//corner left down

	if (isWallFree(w, x - 1, y - 1)
		&& isWallFree(w, x - 1, y)
		&& isWallFree(w, x, y - 1))
	{
		auto& b = *w.getBlockM(x - 1, y - 1);
		b.wall_id[3] = m_id;
		b.wall_corner[3] = BLOCK_STATE_CORNER_DOWN_LEFT;
	}

	//corner right up

	if (isWallFree(w, x + 1, y + 1)
		&& isWallFree(w, x + 1, y)
		&& isWallFree(w, x, y + 1))
	{
		auto& b = *w.getBlockM(x + 1, y + 1);
		b.wall_id[0] = m_id;
		b.wall_corner[0] = BLOCK_STATE_CORNER_UP_RIGHT;
	}

	//corner right down

	if (isWallFree(w, x + 1, y - 1)
		&& isWallFree(w, x + 1, y)
		&& isWallFree(w, x, y - 1))
	{
		auto& b = *w.getBlockM(x + 1, y - 1);
		b.wall_id[2] = m_id;
		b.wall_corner[2] = BLOCK_STATE_CORNER_DOWN_RIGHT;
	}
}

bool Wall::canBePlaced(World& w, int x, int y) const
{
	auto block = w.getBlockM(x, y);
	return block->isWallFree() || BlockRegistry::get().getWall(block->wallID()).isReplaceable();
}
