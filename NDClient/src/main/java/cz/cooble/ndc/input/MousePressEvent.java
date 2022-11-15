package cz.cooble.ndc.input;

import org.joml.Vector2f;

public class MousePressEvent extends MouseEvent{

    private MouseCode m_mouseCode;
    private boolean isRelease;


    public MousePressEvent(Vector2f location, int mods,MouseCode mouseButton,boolean isRelease) {
        super(EventType.MousePress, location, mods);
        m_mouseCode=mouseButton;
        this.isRelease=isRelease;
    }
    public MouseCode getButton() {
        return m_mouseCode;
    }

    public boolean isRelease() {
        return isRelease;
    }
}
