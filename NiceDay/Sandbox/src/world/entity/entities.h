#pragma once

#include "world/entity/WorldEntity.h"
#include "core/physShapes.h"


class PhysEntity : public WorldEntity
{
public:
	enum Blockage
	{
		LEFT,
		RIGHT,
		STUCK,
		NONE,
	};

private:
	bool m_is_on_floor;
	Blockage m_blockage;
protected:
	glm::vec2 m_velocity;
	glm::vec2 m_max_velocity;
	glm::vec2 m_acceleration;
	ndPhys::Polygon m_bound;
	bool m_can_walk=true;


public:
	PhysEntity() = default;
	~PhysEntity() override = default;

	//calculates velocity and position based on acceleration and world colliding blocks
	void computePhysics(World& w);
	bool moveOrCollide(World& w, float dt);
	//regards this as dimensionless structure, checks for collisions only blocks
	bool moveOrCollideOnlyBlocksNoBounds(World& w);
	void computeVelocity(World& w);
	void computeWindResistance(World& w,float windResistance=0.01f);

	void save(nd::NBT& src) override;
	void load(nd::NBT& src) override;

	static bool findBlockIntersections(World& w, const glm::vec2& entityPos, const nd::Phys::Polygon& entityBound);

public:
	glm::vec2& getAcceleration() { return m_acceleration; }
	glm::vec2& getVelocity() { return m_velocity; }
	const ndPhys::Polygon& getCollisionBox() const { return m_bound; }
	bool isOnFloor() const { return m_is_on_floor; }
	Blockage getBlockageState() const { return m_blockage; }
};


class Creature : public PhysEntity
{
protected:
	float m_max_health=1;
	float m_health=1;
	//always normalized
	glm::vec2 m_facing_direction;

	inline void setMaxHealth(float maxHealth)
	{
		m_max_health = maxHealth;
		m_health = maxHealth;
	}

public:
	Creature();
	virtual ~Creature() = default;

	virtual void onHit(World& w,WorldEntity *e, float damage){
		m_health = glm::max(0.f, m_health - damage);
		if (m_health == 0.f)
			markDead();
	}

	inline void setFacingDirection(const glm::vec2& facing) { m_facing_direction = facing; }
	inline const glm::vec2& getFacingDirection() const { return m_facing_direction; }
	void save(nd::NBT& src) override;
	void load(nd::NBT& src) override;
};

