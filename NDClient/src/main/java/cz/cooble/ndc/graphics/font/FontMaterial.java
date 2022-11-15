package cz.cooble.ndc.graphics.font;

import cz.cooble.ndc.graphics.Texture;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

import static cz.cooble.ndc.Asserter.*;

public class FontMaterial {

    private static int currentID;
    public final int id;

    FontMaterial() {
        id = currentID++;
    }

    public Texture texture;
    public Font font;
    public String name;

    public Vector4f color;
    public Vector4f border_color;

    public static class Lib {
        private static final Map<String, FontMaterial> s_fonts=new HashMap<>();

        // retrives already existing material or loads it
        public static FontMaterial getMaterial(String name) {
            name = ND_RESLOC(name);
            var d = s_fonts.get(name);
            if (d != null)
                return d;

            var font = Font.Parser.parse(name);
            if (font == null) {
                ND_WARN("Cannot parse fontMat: {}", name);
                return null;
            }
            s_fonts.put(name, new FontMaterial());
            FontMaterial mat = s_fonts.get(name);
            mat.font = font;
            mat.texture = new Texture(
                    new Texture.Info(
                            (name.startsWith("res/fonts/") ? "res/fonts/" : "") + mat.font.texturePath)
                            .filterMode(Texture.FilterMode.LINEAR));

            mat.color = new Vector4f(1, 1, 1, 1);
            mat.border_color = new Vector4f(0, 0.1f, 0.7f, 1);
            mat.name = name;

            ND_TRACE("Loaded fontMaterial: {}", name);
            return mat;
        }
    };
};


