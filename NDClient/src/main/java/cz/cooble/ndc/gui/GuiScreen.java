package cz.cooble.ndc.gui;

import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.graphics.Renderer;
import cz.cooble.ndc.input.Event;

import java.util.ArrayList;
import java.util.List;

public class GuiScreen extends GuiElement  {


    List<GuiElement> elements = new ArrayList<>();


    public void addElement(GuiElement e){
        elements.add(e);
    }

    @Override
    public void render(BatchRenderer2D renderer2D) {
        for(var e:elements) {
            if(!e.enabled)
                continue;
            e.render(renderer2D);
            renderer2D.flush();
            renderer2D.begin(Renderer.getDefaultFBO());
        }
    }

    @Override
    public void onUpdate() {
        for(var e:elements) {
            if(!e.enabled)
                continue;
            e.onUpdate();
        }
    }

    @Override
    public void onEvent(Event e) {
        for (var w : elements) {
            if(!w.enabled)
                continue;
            w.onEvent(e);
            if(e.handled)
                break;
        }
    }
}
