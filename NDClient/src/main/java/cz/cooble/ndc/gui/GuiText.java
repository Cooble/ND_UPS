package cz.cooble.ndc.gui;

import cz.cooble.ndc.graphics.BatchRenderable2D;
import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.graphics.font.FontMaterial;
import cz.cooble.ndc.graphics.font.TextBuilder;
import cz.cooble.ndc.graphics.font.TextMesh;
import org.joml.*;

public class GuiText extends GuiElement {

    FontMaterial fontMaterial;
    TextMesh textMesh;

    int textAlignment = TextBuilder.ALIGN_LEFT;
    String lines = "";

    public GuiText() {
        fontMaterial = FontMaterial.Lib.getMaterial("fonts/andrew_czech.fnt");
        textMesh = new TextMesh(100);
    }

    public void setText(String lines) {
        this.lines = lines;
        textMesh.reserve(lines.length() + 10);
    }

    @Override
    public void render(BatchRenderer2D renderer2D) {

        var list = TextBuilder.convertToLines(lines, fontMaterial.font, 500);
        dim.y = list.size() * fontMaterial.font.getLineHeight();

        TextBuilder.buildMesh(list, fontMaterial.font, textMesh, textAlignment, null, null);
        renderer2D.push(new Matrix4f().translate(pos.x, pos.y, 0));
        renderer2D.submitText(textMesh, fontMaterial);
        renderer2D.pop();
        // renderer2D.submitColorQuad(new Vector3f(pos.x, pos.y, 0), new Vector2f(dim.x, dim.y), new Vector4f(0, 0, 0, 0.5f));
    }
}
