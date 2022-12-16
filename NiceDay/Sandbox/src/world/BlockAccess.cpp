#include "ndpch.h"
#include "BlockAccess.h"
#include "World.h"

void BlockAccess::setBlock(int x, int y, int blockid)
{
	auto fukLinux = BlockStruct(blockid);
	setBlock(x, y, fukLinux);
}

void BlockAccess::setBlockWithNotify(int x, int y, int blockid)
{
	auto fukLinux = BlockStruct(blockid);
	setBlockWithNotify(x, y, fukLinux);
}
