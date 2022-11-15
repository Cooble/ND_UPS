package cz.cooble.ndc.input;

import org.joml.Vector2f;

public class MouseScrollEvent extends MouseEvent {

    private final float m_scrollX;
    private final float m_scrollY;

    public MouseScrollEvent(Vector2f position, int mods, float scrollX, float scrollY) {
        super(EventType.MouseScroll, position, mods);
        m_scrollX = scrollX;
        m_scrollY = scrollY;
    }

    public float getScrollX() {
        return m_scrollX;
    }
    public float getScrollY() {
        return m_scrollY;
    }
}
