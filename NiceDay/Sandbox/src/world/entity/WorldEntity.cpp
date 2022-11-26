#include "ndpch.h"
#include "WorldEntity.h"
#include "world/World.h"
#include "core/NBT.h"

using namespace nd;


void WorldEntity::save(NBT& src)
{
	src.save("entityPos", m_pos);
	src.save("entityTypeID", getEntityType());
	src.save("entityID", m_id);
}

void WorldEntity::load(NBT& src)
{
	src.load("entityPos", m_pos);
	src.load("entityID", m_id);
}
//TileEntity====================================================
