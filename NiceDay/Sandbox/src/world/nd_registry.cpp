#include "nd_registry.h"
#include "block/BlockRegistry.h"
#include "block/Block.h"
#include "block/basic_walls.h"
#include "block/basic_blocks.h"
#include "entity/EntityRegistry.h"
#include "entity/entities.h"
#include "entity/EntityPlayer.h"

#include "block/block_datas.h"

using namespace nd;

constexpr int particleAtlasSize = 8;
constexpr int ITEM_ATLAS_SIZE = 16;

void nd_registry::registerEverything()
{
	ND_TRACE("Registering: Blocks");
	registerBlocks();
}

void nd_registry::registerBlocks()
{
	ND_TRACE("Loading block id list");
	BlockRegistry::get().readExternalIDList();

	//blocks
	ND_REGISTER_BLOCK(new BlockAir());
	ND_REGISTER_BLOCK(new BlockStone());
	ND_REGISTER_BLOCK(new BlockDirt());
	ND_REGISTER_BLOCK(new BlockSnow());
	ND_REGISTER_BLOCK(new BlockSnowBrick());
	ND_REGISTER_BLOCK(new BlockIce());
	ND_REGISTER_BLOCK(new BlockGold());

	//walls
	ND_REGISTER_WALL(new WallAir());
	ND_REGISTER_WALL(new WallStone());
	ND_REGISTER_WALL(new WallDirt());
	ND_REGISTER_WALL(new WallSnow());
	
}

void nd_registry::registerEntities()
{

}