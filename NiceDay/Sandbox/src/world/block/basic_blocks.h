#pragma once
#include "Block.h"

class BlockTextureAtlas;

class BlockAir : public Block
{
public:
	BlockAir();
	bool onNeighborBlockChange(World& world, int x, int y) const override;
};

class BlockGrass : public Block
{
public:
	BlockGrass();
	bool onNeighborBlockChange(World& world, int x, int y) const override;
};

class BlockGlass : public Block
{
public:
	BlockGlass();
	bool onNeighborBlockChange(World& world, int x, int y) const override;
};