package cz.cooble.ndc.world;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.core.Pair;
import cz.cooble.ndc.graphics.BatchRenderable2D;
import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.graphics.font.Font;
import cz.cooble.ndc.graphics.font.FontMaterial;
import cz.cooble.ndc.graphics.font.TextBuilder;
import cz.cooble.ndc.graphics.font.TextMesh;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.KeyPressEvent;
import cz.cooble.ndc.input.KeyTypeEvent;
import cz.cooble.ndc.net.Timeout;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class GuiConsole implements BatchRenderable2D {

    FontMaterial fontMaterial;
    TextMesh textMesh;
    TextMesh textMeshLine;

    public static final int LINE_TIMEOUT = 10000;
    public static final int MAX_LINES = 10;

    List<Pair<String, Timeout.Pocket>> lines = new ArrayList<>();
    private String currentLine = "";
    boolean editMode;

    private Consumer<String> onEnter = s -> {};

    public GuiConsole() {
        fontMaterial = FontMaterial.Lib.getMaterial("fonts/andrew_czech.fnt");
        textMesh = new TextMesh(2000);
        textMeshLine = new TextMesh(300);
    }

    public void setOnEnter(Consumer<String> onEnter) {
        this.onEnter = onEnter;
    }

    public void addLine(String line) {
        var p = new Pair<>(line, new Timeout.Pocket(LINE_TIMEOUT));
        p.getSecond().start();
        lines.add(p);

        if (lines.size() > MAX_LINES)
            lines.remove(0);
    }


    public boolean isEditMode() {
        return editMode;
    }


    boolean start;

    public void onEvent(Event e) {

        if (e instanceof KeyPressEvent) {
            e.markHandled();
            if (((KeyPressEvent) e).isRelease())
                return;
            switch (((KeyPressEvent) e).getKeycode()) {
                case GLFW_KEY_BACKSPACE:
                {
                    if (!currentLine.isEmpty())
                        currentLine = currentLine.substring(0, currentLine.length() - 1);
                }break;
                case GLFW_KEY_ENTER: {
                    enter();
                    start = false;
                }break;
                case GLFW_KEY_ESCAPE: {
                    editMode = false;
                    currentLine = "";
                    start = false;
                }break;
            }
        }

        if (e instanceof KeyTypeEvent) {
            e.markHandled();
            if (!start) {
                start = true;
                return;//consume first key which was used to open console
            }
            if (((KeyTypeEvent) e).getKeycode() != GLFW_KEY_UNKNOWN)
                currentLine += (char) ((KeyTypeEvent) e).getKeycode();
        }
    }

    public void enter() {
        if (!currentLine.isEmpty()) {
            addLine(currentLine);
            onEnter.accept(currentLine);
        }
        currentLine = "";
        editMode = false;
    }

    int blinking;
    final int BLINK_INTERVAL_HALF = 30;

    boolean isBlink() {return blinking > 0;}

    public void update() {
        if (blinking-- < -BLINK_INTERVAL_HALF) {
            blinking += 2 * BLINK_INTERVAL_HALF;
        }

        // while (!lines.isEmpty() && lines.get(0).getSecond().isTimeout())
        //     lines.remove(0);
    }


    String serverColor = Font.colorize(Font.BLACK, Font.RED);
    String playerColor = Font.colorize(Font.BLACK, Font.WHITE);
    String chatColor = Font.colorize(Font.BLUE, Font.WHITE);
    String commandColor = Font.colorize(Font.BLACK, Font.GREEN);

    private String stylish(String s) {
        if (s.isEmpty())
            return chatColor + s;
        if (s.charAt(0) == '/')
            return commandColor + s;
        if (s.charAt(0) == '[') {
            if (s.indexOf(']') == -1)
                return chatColor + s;
            var prepStart = s.indexOf(']');
            String name = s.substring(1, prepStart);
            if (name.equals("server"))
                return serverColor + s;
            else
                return playerColor + s.substring(0, prepStart + 1) + chatColor + s.substring(prepStart + 1);
        }
        return chatColor + s;
    }

    @Override
    public void render(BatchRenderer2D renderer2D) {
        List<String> out = new ArrayList<>();
        int lineCount = lines.size() + (isEditMode() ? 1 : 0);
        for (var e : lines)
            if (!e.getSecond().isTimeout() || isEditMode())
                out.add(stylish(e.getFirst()));

        if (isEditMode())
            out.add(stylish(currentLine + (isBlink() ? "_" : "")));
        else out.add("");// one line without nothing

        var offset = fontMaterial.font.getLineHeight() * 0.05f;
        TextBuilder.buildMesh(out, fontMaterial.font, textMesh, TextBuilder.ALIGN_LEFT, null, null);

        renderer2D.push(new Matrix4f().translate(offset * 4, (out.size() - 1) * fontMaterial.font.getLineHeight() + offset, 0));
        renderer2D.submitText(textMesh, fontMaterial);
        renderer2D.pop();
        if (editMode)
            renderer2D.submitColorQuad(new Vector3f(offset, offset, 0), new Vector2f(App.get().getWindow().getWidth() - 2 * offset, fontMaterial.font.getLineHeight() + offset * 2), new Vector4f(0, 0, 0, 0.5f));
    }

    public void openEditMode() {
        editMode = true;
        currentLine = "";
    }
}
