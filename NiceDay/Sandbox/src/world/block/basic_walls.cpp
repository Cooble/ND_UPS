#include "ndpch.h"
#include "basic_walls.h"
#include "block_datas.h"

//AIR=============================
WallAir::WallAir()
	: Wall("air")
{
	setFlag(BLOCK_FLAG_HAS_ITEM_VERSION, false);
}


WallGlass::WallGlass()
	: Wall("glass") {}


