package cz.cooble.ndc.gui;

import cz.cooble.ndc.graphics.BatchRenderable2D;
import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.graphics.font.Font;
import cz.cooble.ndc.graphics.font.FontMaterial;
import cz.cooble.ndc.graphics.font.TextBuilder;
import cz.cooble.ndc.graphics.font.TextMesh;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.KeyPressEvent;
import cz.cooble.ndc.input.KeyTypeEvent;
import cz.cooble.ndc.input.MousePressEvent;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import javax.swing.plaf.PanelUI;
import java.util.*;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class GuiTextBox extends GuiElement {

    public String getText() {
        return line;
    }

    public void setText(String s) {
        line=s;

    }

    public interface IsValidChar {
        boolean isValid(int c);
    }

    public static final IsValidChar ALL_VALIDATOR = (c) -> true;

    public static final IsValidChar IP_VALIDATOR = (c) -> {
        if (c >= '0' && c <= '9')
            return true;
        if (c == '.')
            return true;
        return c == ':';
    };

    FontMaterial fontMaterial;
    TextMesh textMesh;
    TextMesh textMeshLine;

    public int maxLength = -1;
    public IsValidChar isValidChar = ALL_VALIDATOR;

    private String line = "";
    boolean editMode;

    private Consumer<String> onEnter = s -> {};

    public GuiTextBox() {
        fontMaterial = FontMaterial.Lib.getMaterial("fonts/andrew_czech.fnt");
        textMesh = new TextMesh(1000);
        textMeshLine = new TextMesh(300);
    }

    public void setOnEnter(Consumer<String> onEnter) {
        this.onEnter = onEnter;
    }


    public boolean isEditMode() {
        return editMode;
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof MousePressEvent && ((MousePressEvent) e).isRelease()) {
            if (contains((int) ((MousePressEvent) e).getX(), (int) ((MousePressEvent) e).getY())) {
                editMode = !editMode;
                e.handled = true;
                System.out.println(editMode);
                return;
            }
        }
        if (!editMode)
            return;
        e.handled = true;
        if (e instanceof KeyPressEvent) {
            e.markHandled();
            if (((KeyPressEvent) e).isRelease())
                return;
            switch (((KeyPressEvent) e).getKeycode()) {
                case GLFW_KEY_BACKSPACE -> {
                    if (!line.isEmpty())
                        line = line.substring(0, line.length() - 1);
                }
                case GLFW_KEY_ENTER -> {
                    enter();
                }
                case GLFW_KEY_ESCAPE -> {
                    editMode = false;
                    line = "";
                }
            }
        }

        if (e instanceof KeyTypeEvent) {
            e.markHandled();
            if ((maxLength == -1 || line.length() != maxLength) &&
                    isValidChar.isValid(((KeyTypeEvent) e).getKeycode()))
                if (((KeyTypeEvent) e).getKeycode() != GLFW_KEY_UNKNOWN)
                    line += (char) ((KeyTypeEvent) e).getKeycode();
        }
    }

    public void enter() {
        onEnter.accept(line);
        editMode = false;
    }

    int blinking;
    final int BLINK_INTERVAL_HALF = 30;

    boolean isBlink() {return blinking > 0;}

    @Override
    public void onUpdate() {
        if (blinking-- < -BLINK_INTERVAL_HALF) {
            blinking += 2 * BLINK_INTERVAL_HALF;
        }
    }

    String chatColor = Font.colorize(Font.BLUE, Font.WHITE);

    private String stylish(String s) {
        return chatColor + s;
    }

    @Override
    public void render(BatchRenderer2D renderer2D) {
        ArrayList<String> out = new ArrayList<>();
        out.add(stylish(line + (isBlink() && editMode ? "_" : "")));

        var offset = fontMaterial.font.getLineHeight() * 0.05f;
        TextBuilder.buildMesh(out, fontMaterial.font, textMesh, TextBuilder.ALIGN_LEFT, null, null);

        renderer2D.push(new Matrix4f().translate(pos.x + offset*4, pos.y + offset*4, 0));
        renderer2D.submitText(textMesh, fontMaterial);
        renderer2D.pop();
        renderer2D.submitColorQuad(new Vector3f(pos.x, pos.y, 0), new Vector2f(dim.x, dim.y), new Vector4f(0, 0, 0, 0.5f));
    }

    public void openEditMode() {
        editMode = true;
    }
}
