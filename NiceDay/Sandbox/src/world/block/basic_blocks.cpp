#include "ndpch.h"
#include "basic_blocks.h"
#include "world/World.h"
#include "block_datas.h"


//AIR=======================================
BlockAir::BlockAir()
	: Block("air")
{
	setNoCollisionBox();
	m_block_connect_group = BIT(BLOCK_GROUP_AIR_BIT);
	setFlag(BLOCK_FLAG_HAS_ITEM_VERSION, false);
	setFlag(BLOCK_FLAG_SOLID, false);
	setFlag(BLOCK_FLAG_REPLACEABLE, true);
}


bool BlockAir::onNeighborBlockChange(World& world, int x, int y) const { return false; }


//STONE=======================================


//GRASS=======================================

BlockGrass::BlockGrass()
	: Block("grass") {}


bool BlockGrass::onNeighborBlockChange(World& world, int x, int y) const
{
	auto& block = *world.getBlockM(x, y);
	int lastid = block.block_id;
	int lastCorner = block.block_corner;
	//bool custombit = (lastCorner & BIT(4)); //perserve custombit
	//block.block_corner &= ~BIT(4);
	bool custombit = lastCorner & BLOCK_STATE_HAMMER; //perserve custombit
	block.block_corner &= ~BLOCK_STATE_HAMMER;


	Block::onNeighborBlockChange(world, x, y); //update corner state
	if (block.block_corner == BLOCK_STATE_FULL
		|| block.block_corner == BLOCK_STATE_LINE_DOWN
		|| block.block_corner == BLOCK_STATE_LINE_LEFT
		|| block.block_corner == BLOCK_STATE_LINE_RIGHT
		|| block.block_corner == BLOCK_STATE_CORNER_DOWN_LEFT
		|| block.block_corner == BLOCK_STATE_CORNER_DOWN_RIGHT
		|| block.block_corner == BLOCK_STATE_LINE_END_DOWN
		|| block.block_corner == BLOCK_STATE_LINE_VERTICAL)
	{
		block.block_id = BLOCK_DIRT; //no air around
		custombit = false; //we dont care about cracks anymore
	}
	else if (block.block_corner == BLOCK_STATE_CORNER_UP_LEFT
		|| block.block_corner == BLOCK_STATE_LINE_END_LEFT)
		block.block_metadata = half_int(x & 1, 0);
	else if (block.block_corner == BLOCK_STATE_CORNER_UP_RIGHT
		|| block.block_corner == BLOCK_STATE_LINE_END_RIGHT)
		block.block_metadata = half_int((x & 1) + 2, 0);
	else
		block.block_metadata = half_int(x & 3, 1);
	block.block_corner |= BLOCK_STATE_HAMMER * custombit; //put back custombit
	return lastCorner != block.block_corner || lastid != block.block_id; //we have a change (or not)
}

//GLASS=======================================

BlockGlass::BlockGlass()
	: Block("glass") {}


bool BlockGlass::onNeighborBlockChange(World& world, int x, int y) const
{
	auto c = Block::onNeighborBlockChange(world, x, y);
	auto& e = *world.getBlockM(x, y);
	e.block_metadata = std::rand() % 10;
	e.block_metadata &= 3;
	return c;
}