package cz.cooble.ndc.gui;

import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.graphics.font.FontMaterial;
import cz.cooble.ndc.graphics.font.TextBuilder;
import cz.cooble.ndc.graphics.font.TextMesh;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.MouseMoveEvent;
import cz.cooble.ndc.input.MousePressEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class GuiButton extends GuiElement {

    FontMaterial fontMaterial;
    TextMesh textMesh;
    private String text;
    public Runnable onClick;

    private static final Vector4f unselectColor = new Vector4f(0.2f, 0.2f, 0.2f, 1);
    private static final Vector4f selectColor = new Vector4f(0.2f, 0.5f, 0.5f, 1);
    private Vector4f color = unselectColor;

    public GuiButton() {
        fontMaterial = FontMaterial.Lib.getMaterial("fonts/andrew_czech.fnt");
        textMesh = new TextMesh(100);
    }

    public void setText(String text) {
        this.text = text;
        textMesh.reserve(text.length());
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof MousePressEvent && ((MousePressEvent) e).isRelease()) {
            if (contains((int) ((MousePressEvent) e).getX(), (int) ((MousePressEvent) e).getY())) {
                if (onClick != null)
                    onClick.run();
                e.markHandled();
            }
        } else if (e instanceof MouseMoveEvent) {

            color = contains((int) ((MouseMoveEvent) e).getX(), (int) ((MouseMoveEvent) e).getY()) ? selectColor : unselectColor;
        }
    }

    @Override
    public void render(BatchRenderer2D renderer2D) {
        TextBuilder.buildMesh(TextBuilder.convertToLines(text), fontMaterial.font, textMesh, TextBuilder.ALIGN_CENTER, null, null);

        renderer2D.push(new Matrix4f().translate(pos.x + dim.x / 2, pos.y + dim.y / 2-fontMaterial.font.getLineHeight()/2, 0));
        renderer2D.submitText(textMesh, fontMaterial);
        renderer2D.pop();

        int padding = 5;
        renderer2D.submitColorQuad(new Vector3f(pos.x, pos.y, 0), new Vector2f(dim.x, dim.y), new Vector4f(0.2f, 0.2f, 0.2f, 1));
        renderer2D.submitColorQuad(new Vector3f(pos.x + padding, pos.y + padding, 0), new Vector2f(dim.x - padding * 2, dim.y - padding * 2), color/*new Vector4f(0.1f, 0.2f, 0.8f, 1)*/);
    }
}
