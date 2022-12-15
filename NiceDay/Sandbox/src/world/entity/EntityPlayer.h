#pragma once
#include "entities.h"

class EntityPlayer : public Creature
{
private:
	std::string m_name = "Karel";
	float m_pose = 0;
	float m_last_pose = 0;
	int m_animation_var = 0;
	bool m_has_creative;

	//in hand item
	float m_item_angle= 0;
	bool m_is_swinging = false;
	bool m_is_last_swing = false;
	bool m_is_facing_left;
	bool m_fly=true;

public:
	EntityPlayer();
	virtual ~EntityPlayer() = default;


	void setCreative(bool cre){m_has_creative = cre;}
	bool hasCreative() const { return m_has_creative; }
	void update(World& w) override;
	EntityType getEntityType() const override;
	
	TO_ENTITY_STRING(EntityPlayer)

	void save(nd::NBT& src) override;
	void load(nd::NBT& src) override;

	void setFly(bool f) { m_fly = f; }
	bool isFlying() const { return m_fly; }
};
enum MouseState
{
	PRESS,RELEASE,HOLD,NONE
};
enum MouseButton
{
	LEFT,RIGHT
};
class PlayerInteractor
{
	EntityPlayer* m_player;
public:
	PlayerInteractor(EntityPlayer* player):m_player(player){}
	
	void click(World* w, glm::vec2 pos, MouseButton leftRight, MouseState state);

	void update(World* w);
};
