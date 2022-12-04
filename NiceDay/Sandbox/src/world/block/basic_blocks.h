#pragma once
#include "Block.h"

class BlockAccess;
class BlockTextureAtlas;

class BlockAir : public Block
{
public:
	BlockAir();
	bool onNeighborBlockChange(BlockAccess& world, int x, int y) const override;
};

class BlockStone : public Block
{
public:
	BlockStone();
};
class BlockDirt : public Block
{
public:
	BlockDirt();
};
class BlockSnow : public Block
{
public:
	BlockSnow();
};
class BlockIce : public Block
{
public:
	BlockIce();
};
class BlockGold : public Block
{
public:
	BlockGold();
};
class BlockSnowBrick : public Block
{
public:
	BlockSnowBrick();
};