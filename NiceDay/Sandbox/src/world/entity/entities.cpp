#include "ndpch.h"
#include "entities.h"
#include "entity_datas.h"

#include "world/block/BlockRegistry.h"
#include "world/World.h"
#include "world/block/block_datas.h"
#include "world/block/basic_blocks.h"



#include "EntityAllocator.h"
#include "EntityPlayer.h"
#include "core/NBT.h"

using namespace glm;
using namespace nd;

//PhysEntity======================================================




//return if we have intersection
static bool findBlockIntersection(World& w, const glm::vec2& entityPos, const Phys::Polygon& entityBound)
{
	auto& entityRectangle = entityBound.getBounds();
	for (int x = entityRectangle.x0 - 1; x < ceil(entityRectangle.x1) + 1; ++x)
	{
		for (int y = entityRectangle.y0 - 1; y < ceil(entityRectangle.y1) + 1; ++y)
		{
			glm::vec2 blockPos(x, y);
			blockPos += entityPos;

			auto stru = w.getBlock((int)blockPos.x, (int)blockPos.y);
			if (stru == nullptr || stru->block_id == BLOCK_AIR)
				continue;
			auto& block = BlockRegistry::get().getBlock(stru->block_id);
			if (!block.hasCollisionBox())
				continue;
			auto blockBounds = block.getCollisionBox((int)blockPos.x, (int)blockPos.y, *stru).copy();

			blockBounds.plus({ x - (entityPos.x - (int)entityPos.x), y - (entityPos.y - (int)entityPos.y) });
			if (Phys::isIntersects(blockBounds, entityBound))
				return true;
		}
	}
	return false;
}
bool PhysEntity::findBlockIntersections(World& w, const glm::vec2& entityPos, const nd::Phys::Polygon& entityBound)
{
	return findBlockIntersection(w, entityPos, entityBound);
}

static float getFloorHeight(World& w, const glm::vec2& entityPos, const Phys::Rectangle& entityRectangle)
{
	float lineY = std::numeric_limits<float>::lowest();
	for (int i = 0; i < 2; ++i)
	{
		for (int yy = 0; yy < 2; ++yy)
		{
			float x = entityRectangle.x0 + entityPos.x + i * entityRectangle.width();
			float y = entityRectangle.y0 + entityPos.y + yy;
			auto& pointDown0 = glm::vec2(x, y);
			auto& pointDown1 = glm::vec2(x, y - 10000);

			auto stru = w.getBlock((int)pointDown0.x, (int)pointDown0.y);
			if (stru == nullptr || stru->block_id == BLOCK_AIR)
				continue;
			auto& block = BlockRegistry::get().getBlock(stru->block_id);
			if (!block.hasCollisionBox())
				continue;

			auto blockBounds = block.getCollisionBox((int)pointDown0.x, (int)pointDown0.y, *stru).copy();
			auto blockPos = glm::vec2((int)pointDown0.x, (int)pointDown0.y);
			for (int j = 0; j < blockBounds.size(); ++j)
			{
				auto& v0 = blockBounds[j] + blockPos;
				auto& v1 = blockBounds[(j + 1) % blockBounds.size()] + blockPos;

				if (i == 0) //left
				{
					if (v0.x > pointDown0.x)
						lineY = max(lineY, v0.y);
				}
				else if (v0.x < pointDown0.x) //right
					lineY = max(lineY, v0.y);

				auto v = Phys::intersectLines(v0, v1, pointDown0, pointDown1);
				if (Phys::isValid(v) && v.y < entityRectangle.y1 + entityPos.y)
					lineY = max(lineY, v.y);
			}
		}
	}
	return lineY;
}

constexpr float maxDistancePerStep = 0.4f;

static float sgn(float f)
{
	if (f == 0)
		return 0;
	return abs(f) / f;
}

void PhysEntity::computePhysics(World& w)
{
	
	float lastXAcc = m_acceleration.x;
	

	computeVelocity(w);

	m_acceleration.x = lastXAcc;

	computeWindResistance(w);

	float lengthCab = glm::length(m_velocity);

	if (lengthCab > maxDistancePerStep)
	{
		float dividor = ceil(lengthCab / maxDistancePerStep);
		for (int i = 0; i < dividor; ++i)
		{
			if (moveOrCollide(w, 1.0f / dividor))
				break;
		}
	}
	else moveOrCollide(w, 1);
}

bool PhysEntity::moveOrCollide(World& w, float dt)
{
	m_blockage = NONE;
	m_is_on_floor = false;

	bool isSlippery = (w.getBlock(m_pos.x, m_pos.y - 1) != nullptr && w.getBlock(m_pos.x, m_pos.y - 1)->block_id ==
		BLOCK_ICE)
		|| (w.getBlock(m_pos.x, m_pos.y) != nullptr && w.getBlock(m_pos.x, m_pos.y)->block_id == BLOCK_ICE);
	float floorResistance = isSlippery ? 0.005f : 0.2f; //negative numbers mean conveyor belt Yeah!


	//collision detection============================
	auto possibleEntityPos = m_pos + (m_velocity * dt);

	//check if we can procceed in x, y direction
	if (findBlockIntersection(w, possibleEntityPos, m_bound))
	{
		possibleEntityPos = m_pos;
		possibleEntityPos.x += m_velocity.x * dt;
		//check if we can procceed at least in x direction
		if (findBlockIntersection(w, possibleEntityPos, m_bound))
		{
			//walk on floor check
			if (m_can_walk && m_bound.isRectangle)
			{
				float y0 = m_bound.getBounds().y0 + m_pos.y;
				float floorHeight = getFloorHeight(w, possibleEntityPos, m_bound.getBounds());
				float difference = floorHeight - y0;

				constexpr float maxHeightToWalk = 0.99f;
				if (floorHeight < (m_bound.getBounds().y1 + m_pos.y) &&
					difference > -0.06f &&
					difference < maxHeightToWalk)
				{
					possibleEntityPos.y = floorHeight + 0.05f + m_bound.getBounds().y0;
					if (!findBlockIntersection(w, possibleEntityPos, m_bound))
					{
						m_is_on_floor = true;
						m_pos = possibleEntityPos;
						m_velocity.y = 0;

						if (m_velocity.x > 0) //apply resistance
						{
							m_velocity.x -= floorResistance / 2;
							if (m_velocity.x < 0)
								m_velocity.x = 0;
						}
						else if (m_velocity.x < 0)
						{
							m_velocity.x += floorResistance / 2;
							if (m_velocity.x > 0)
								m_velocity.x = 0;
						}

						return false;
					}
				}
			}

			possibleEntityPos = m_pos;
			possibleEntityPos.y += m_velocity.y * dt;
			//check if we can procceed at least in y direction
			if (findBlockIntersection(w, possibleEntityPos, m_bound))
			{
				//we have nowhere to go
				m_blockage = m_velocity.x > 0 ? RIGHT : LEFT;
				if (m_velocity.y <= 0)
				{
					m_is_on_floor = true;
					m_pos.y = (int)m_pos.y + 0.01f;
				}
				possibleEntityPos = m_pos;
				m_velocity = { 0, 0 };
			}
			else
			{
				//we can proceed in y direction
				m_velocity.x = 0;
			}
		}
		else //we can procceed in x direction
		{
			if (m_velocity.y <= 0)
			{
				if (m_bound.isRectangle)
				{
					float floorHeight = getFloorHeight(w, possibleEntityPos, m_bound.getBounds());
					if (Phys::isValidFloat(floorHeight) && w.isBlockValid(possibleEntityPos.x, floorHeight) && abs(
						floorHeight - m_pos.y) < 0.5)
						possibleEntityPos.y = floorHeight + 0.01f;
				}
				m_is_on_floor = true;
			}
			m_velocity.y = 0;


			if (m_velocity.x > 0)
			{
				m_velocity.x -= floorResistance;
				if (m_velocity.x < 0)
					m_velocity.x = 0;
			}
			else if (m_velocity.x < 0)
			{
				m_velocity.x += floorResistance;
				if (m_velocity.x > 0)
					m_velocity.x = 0;
			}
		}
	}
	m_pos = possibleEntityPos;
	return false;
}

bool PhysEntity::moveOrCollideOnlyBlocksNoBounds(World& w)
{
	auto possiblePos = m_pos + m_velocity;
	auto b = w.getBlock(possiblePos.x, possiblePos.y);
	if (b)
	{
		auto& block = BlockRegistry::get().getBlock(b->block_id);
		if (block.hasCollisionBox())
		{
			auto& blockBounds = block.getCollisionBox((int)possiblePos.x, (int)possiblePos.y, *b);
			if (Phys::contains(blockBounds,
				{ (possiblePos.x - (int)possiblePos.x), (possiblePos.y - (int)possiblePos.y) }))
				return false;
		}
	}
	m_pos = possiblePos;
	return true;
}

void PhysEntity::computeVelocity(World& w)
{
	//motion=========================================
	m_velocity += m_acceleration;
	m_velocity = glm::clamp(m_velocity, -m_max_velocity, m_max_velocity);
}

void PhysEntity::computeWindResistance(World& w, float windResistance)
{
	//wind resistance================================
	//constexpr float windResistance = 0.01;
	constexpr float windVelocityDepenceFactor = 1000000; //we dont need higher air resistance the bigger the velocity

	if (m_velocity.x > 0)
	{
		m_velocity.x -= windResistance + m_velocity.x / windVelocityDepenceFactor;
		if (m_velocity.x < 0)
			m_velocity.x = 0;
	}
	else if (m_velocity.x < 0)
	{
		m_velocity.x += windResistance - m_velocity.x / windVelocityDepenceFactor;
		if (m_velocity.x > 0)
			m_velocity.x = 0;
	}
}

void PhysEntity::save(NBT& src)
{
	WorldEntity::save(src);
	src.save("velocity", m_velocity);
	src.save("acceleration", m_acceleration);
}

void PhysEntity::load(NBT& src)
{
	WorldEntity::load(src);
	src.load("velocity", m_velocity, glm::vec2(0, 0));
	src.load("acceleration", glm::vec2(0, -9.8f / 60));
}



Creature::Creature()
{
	m_flags |= EFLAG_COLLIDER;
}


void Creature::save(NBT& src)
{
	PhysEntity::save(src);
	src.save("Health", m_health);
}

void Creature::load(NBT& src)
{
	PhysEntity::load(src);
	src.load("Health", m_health, m_max_health);
}
