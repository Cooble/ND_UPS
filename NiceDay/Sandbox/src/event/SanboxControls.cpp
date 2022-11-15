#include "SandboxControls.h"
#include "event/KeyEvent.h"

using namespace nd;

ControlType Controls::SPAWN_ENTITY=			(ControlType)KeyCode::E;
ControlType Controls::DROP_ITEM =			(ControlType)KeyCode::Q;
ControlType Controls::OPEN_CONSOLE =		(ControlType)KeyCode::F;
ControlType Controls::OPEN_INVENTORY =		(ControlType)KeyCode::I;
ControlType Controls::GO_UP =				(ControlType)KeyCode::UP;
ControlType Controls::GO_DOWN =				(ControlType)KeyCode::DOWN;
ControlType Controls::GO_LEFT =				(ControlType)KeyCode::LEFT;
ControlType Controls::GO_RIGHT =			(ControlType)KeyCode::RIGHT;
ControlType Controls::SPAWN_TNT =			(ControlType)KeyCode::T;
ControlType Controls::SPAWN_BULLETS =		(ControlType)KeyCode::B;
ControlType Controls::FLY_MODE =			(ControlType)KeyCode::C;
ControlType Controls::AUTO_BLOCK_PICKER =			(ControlType)KeyCode::V;

void Controls::init()
{
	using namespace Controls;
}
