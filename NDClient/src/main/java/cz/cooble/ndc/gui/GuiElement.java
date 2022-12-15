package cz.cooble.ndc.gui;

import cz.cooble.ndc.graphics.BatchRenderable2D;
import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.input.Event;
import org.joml.Vector2i;

public class GuiElement implements BatchRenderable2D {

    public Vector2i pos = new Vector2i();
    public Vector2i dim = new Vector2i();

    public boolean enabled=true;

    @Override
    public void render(BatchRenderer2D renderer2D) {}

    public void onUpdate() {}

    public void onEvent(Event e) {}

    public final boolean contains(int x, int y) {
        return x >= pos.x && x <= pos.x + dim.x && y >= pos.y && y <= pos.y + dim.y;
    }
}
