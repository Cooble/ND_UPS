package cz.cooble.ndc.input;

import org.joml.Vector2f;

public class MouseEvent extends Event {


    protected Vector2f location;

    public MouseEvent(EventType type,Vector2f location,int mods) {
        super(type,mods);
        this.location = location;
    }

    public Vector2f getLocation() {
        return location;
    }
    void flipY(float screenHeight)
    {
        location.y = screenHeight - 1 - location.y;
    }

    public float getX() { return location.x; }
    public float getY() { return location.y; }
}
