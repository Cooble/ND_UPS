#include "EntityPlayer.h"
#include "core/NBT.h"

#include "entity_datas.h"

//PlayerEntity======================================================


using namespace glm;
using namespace nd;

EntityPlayer::EntityPlayer()
{
	m_is_item_consumer = true;
	m_velocity = vec2(0.0f);
	m_acceleration = {0.f, -9.8f / 60 / 2};
	m_max_velocity = vec2(40.f / 60);


	m_bound = Phys::toPolygon(Phys::Rectangle::createFromDimensions(-0.75f, 0, 1.5f, 2.9f));
}

void EntityPlayer::update(World& w)
{
	constexpr float swingSpeed = 10;
	if (m_is_swinging)
		m_item_angle += m_is_facing_left ? swingSpeed : -swingSpeed;
	if (m_is_last_swing)
		if (m_item_angle >= 360 || m_item_angle <= -360)
		{
			m_is_swinging = false;
			m_is_last_swing = false;
		}
	auto lastPos = m_pos;
	auto lastFacingLeft = m_is_facing_left;
	if (!m_fly)
		PhysEntity::computePhysics(w);
	else
	{
		float len = glm::length(m_velocity);
		if (len > m_max_velocity.x)
			m_velocity = glm::normalize(m_velocity) * m_max_velocity.x;
		//m_velocity = glm::clamp(m_velocity, -m_max_velocity, m_max_velocity);
		m_pos += m_velocity;
	}
}


EntityType EntityPlayer::getEntityType() const { return ENTITY_TYPE_PLAYER; }

void EntityPlayer::save(NBT& src)
{
	Creature::save(src);
	src.save("playerName", m_name);
}

void EntityPlayer::load(NBT& src)
{
	Creature::load(src);
	src.load("playerName", m_name, std::string("Karel"));
}
