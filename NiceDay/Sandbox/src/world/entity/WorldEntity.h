#pragma once
#include "ndpch.h"
#include "EntityManager.h"
#include "world/WorldTime.h"

namespace nd {
class NBT;
}

constexpr int EFLAG_TEMPORARY =		BIT(0);//will be killed on chunk unload
constexpr int EFLAG_CHUNK_LOADER =	BIT(1);//will keep chunks around loaded (usually Player)
constexpr int EFLAG_COLLIDER  =		BIT(2);//will collide with other entities (will be pushed by them)

class World;
struct WorldTime;

class WorldEntity
{
	friend World;
private:
	bool m_is_dead=false;
protected:
	bool m_is_item_consumer = false;
	EntityID m_id;// each entity instance has unique id
protected:
	uint64_t m_flags=0;
	glm::vec2 m_pos;//world pos
protected:
	WorldEntity() = default;
	virtual ~WorldEntity() = default;
public:
	WorldEntity(const WorldEntity&) = delete;
	inline bool hasFlag(uint64_t flags) const { return (m_flags & flags)==flags; }

	inline bool isMarkedDead()const { return m_is_dead; }

	inline EntityID getID() const { return m_id; }
	
	inline void markDead() { m_is_dead = true; }//used for suicide (no one else can call this method)

	virtual EntityType getEntityType() const = 0;

	inline const glm::vec2& getPosition() const { return m_pos; }
	inline glm::vec2& getPosition() { return m_pos; }
	inline glm::vec2 getNPos() { return m_pos; }
	inline bool isItemConsumer() const { return m_is_item_consumer; }
	
	

	virtual void update(World& w) {}

	virtual void onLoaded(World& w) {}
	virtual void onUnloaded(World& w) {}

	virtual void onSpawned(World& w){}
	virtual void onKilled(World& w){}


	virtual void save(nd::NBT& src);
	virtual void load(nd::NBT& src);

	inline virtual std::string toString() const { return "UNDEFINED_ENTITY"; }

#define TO_ENTITY_STRING(x)\
	inline std::string toString() const override {return #x;}


};

struct BlockStruct;



