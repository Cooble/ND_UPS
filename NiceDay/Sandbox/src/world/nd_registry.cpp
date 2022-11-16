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
	
	//blocks
	ND_REGISTER_BLOCK(new BlockAir());
	ND_REGISTER_BLOCK(new BlockGrass());
	ND_REGISTER_BLOCK(new BlockGlass());

	//walls
	ND_REGISTER_WALL(new WallAir());
	ND_REGISTER_WALL(new WallGlass());
	
}

void nd_registry::registerEntities()
{

}