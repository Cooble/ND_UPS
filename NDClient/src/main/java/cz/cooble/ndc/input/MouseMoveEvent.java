package cz.cooble.ndc.input;

import org.joml.Vector2f;

public class MouseMoveEvent extends MouseEvent{

    public MouseMoveEvent(Vector2f location) {
        super(EventType.MouseMove, location,0);
    }
}
