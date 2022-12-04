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


bool BlockAir::onNeighborBlockChange(BlockAccess& world, int x, int y) const { return false; }


//STONE=======================================


//GRASS=======================================

BlockStone::BlockStone(): Block("stone") {}
BlockDirt::BlockDirt() : Block("dirt") {}
BlockSnow::BlockSnow() : Block("snow") {}
BlockIce::BlockIce() : Block("ice") {}
BlockGold::BlockGold() : Block("gold") {}
BlockSnowBrick::BlockSnowBrick() : Block("snow_brick") {}

