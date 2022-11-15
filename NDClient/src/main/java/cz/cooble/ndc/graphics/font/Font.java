package cz.cooble.ndc.graphics.font;

import cz.cooble.ndc.core.IOUtils;
import cz.cooble.ndc.core.Utils;
import org.joml.Vector4f;

import java.util.*;

import static cz.cooble.ndc.Asserter.*;
import static cz.cooble.ndc.core.Utils.half_int;

public class Font {
    static class Charac {
        int id;
        float u, v;
        float u1, v1;
        int xoffset, yoffset;
        int width, height;
        int xadvance;
    }

    ;
    static Charac nullChar = new Charac();

    // utf-8 font with maaany characters
// has its size, settings as bold/italic (read-only)
// all methods work with utf-8 encoded strings
    int id;

    String face;
    String texturePath;
    int size;
    boolean bold;
    boolean italic;
    int[] padding = new int[4];
    int[] spacing = new int[2];
    int lineHeight;
    int scaleW, scaleH;
    int base;
    float xSpace = 0, ySpace = 0;
    float xThickness = 0.5f, yThickness = 0;
    Map<Integer, Charac> chars=new HashMap<>();
    Map<Integer, Integer> kerning=new HashMap<>();



    int getKerning(int firstIndex, int secondIndex) {
        Integer out = kerning.get(half_int(firstIndex, secondIndex));
        if (out == null)
            return 0;
        return out;
    }

    void setKerning(int firstIndex, int secondIndex, int amount) {
        kerning.put(half_int(firstIndex, secondIndex), amount);
    }


    Charac getChar(int id) {
        var out = chars.get(id);
        if (out == null)
            return nullChar;
        return out;
    }

    int getTextWidth(String text) {
        String textt = Font.removeColorEntities(text);
        int out = 0;
        int lastC = 0;

        for (char codePoint : textt.toCharArray()) {
            //skip blank characters
            if (codePoint < 32) continue;

            out += getChar(codePoint).xadvance + xSpace + getKerning(lastC, codePoint);
            lastC = codePoint;
        }
        return out;
    }

    //wont work on textures that are not square
    float getPixelRatio() {
        return 1.f / scaleW;
    }


    static int colorToInt(Vector4f vec) {
        return (
                ((char) (255.0f * vec.x) << 24) |
                        ((char) (255.0f * vec.y) << 16) |
                        ((char) (255.0f * vec.z) << 8) |
                        ((char) (255.0f * vec.w) << 0)
        );
    }

    static String colorize(String borderColor, String color) {
        return BORDER_PREFIX + borderColor + color;
    }

    static String colorizeBorder(String borderColor) {
        return BORDER_PREFIX + borderColor;
    }


    public static final String DARK_RED = "&0";
    public static final String RED = "&1";
    public static final String GOLD = "&2";
    public static final String YELLOW = "&3";
    public static final String DARK_GREEN = "&4";
    public static final String GREEN = "&5";
    public static final String AQUA = "&6";
    public static final String DARK_AQUA = "&7";
    public static final String DARK_BLUE = "&8";
    public static final String BLUE = "&9";
    public static final String LIGHT_PURPLE = "&a";
    public static final String DARK_PURPLE = "&b";
    public static final String WHITE = "&c";
    public static final String GREY = "&d";
    public static final String DARK_GREY = "&e";
    public static final String BLACK = "&f";

    //the color after prefix is borderColor

    public static final char BORDER_PREFIX = 'b';
    //the color after prefix will be print normally (no color conversion)

    public static final char IGNORE_PREFIX = 'i';


    static int[] colorTemplates = new int[]{
            //0 dark red
            0xbe000000,
            //1 red
            0xfe3f3f00,
            //2 gold
            0xd9a33400,
            //3 yellow
            0xfefe3f00,
            //4 dark green
            0x00be0000,
            //5 green
            0x3ffe3f00,
            //6 aqua
            0x3ffefe00,
            //7 dark aqua
            0x00bebe00,
            //8 dark blue
            0x0000be00,
            //9 blue
            0x3f3ffe00,
            //a light purple
            0xfe3ffe00,
            //b dark purple
            0xbe00be00,
            //c white
            0xffffff00,
            //d grey
            0xbebebe00,
            //e dark grey
            0x3f3f3f00,
            //f black
            0x00000000
    };

    int entityToColor(String s) {
        if (s.length() < 2)
            return 0;

        char c = s.charAt(0);
        char cc = s.charAt(1);
        switch (c) {
            case '&':
                if (cc >= '0' && cc <= '9')
                    return colorTemplates[cc - '0'];
                if (cc >= 'a' && cc <= 'f')
                    return colorTemplates[(cc - 'a') + 10];
                if (cc >= 'A' && cc <= 'F')
                    return colorTemplates[(cc - 'A') + 10];
                return 0;
            case '#':
                if (s.length() < 7)
                    return 0;
                return (Integer.parseInt(s.substring(1), 16) << 8) & 0xffffff00;
        }
        return 0;
    }

    static Integer tryEntityToColor(String s) {
        if (s.length() < 2)
            return null;

        char c = s.charAt(0);
        switch (c) {
            case '&': {
                char cc = s.charAt(1);

                if (cc >= '0' && cc <= '9')
                    return colorTemplates[cc - '0'];
                if (cc >= 'a' && cc <= 'f')
                    return colorTemplates[(cc - 'a') + 10];
                if (cc >= 'A' && cc <= 'F')
                    return colorTemplates[(cc - 'A') + 10];
                return null;
            }
            case '#':
                if (s.length() < 7)
                    return null;
                try {
                    return (Integer.parseInt(s.substring(1, 6), 16) << 8) & 0xffffff00;
                } catch (Exception ignored) {
                    return null;
                }
        }
        return null;
    }

    static String removeColorEntities(String s) {
        String out = "";

        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            boolean isBorderPrefix = false;
            boolean isIgnorePrefix = false;
            if ((c == '&' || c == '#') && i != 0) {
                isBorderPrefix = s.charAt(i - 1) == Font.BORDER_PREFIX;
                isIgnorePrefix = s.charAt(i - 1) == Font.IGNORE_PREFIX;
            }
            if (c == '&') {
                var o = tryEntityToColor(s.substring(i));
                if (o != null) {
                    if (isBorderPrefix || isIgnorePrefix)
                        out = out.substring(0, out.length() - 1); //remvoe even the borderprefix
                    if (!isIgnorePrefix) {
                        if (i > s.length() - 1)
                            return out;
                        i += 1;
                        continue;
                    }
                }
            } else if (c == '#') {
                var o = tryEntityToColor(s.substring(i));
                if (o != null) {
                    if (isBorderPrefix || isIgnorePrefix)
                        out = out.substring(0, out.length() - 2); //remvoe even the borderprefix
                    if (!isIgnorePrefix) {
                        if (i > s.length() - 7)
                            return out;
                        i += 6;
                        continue;
                    }
                }
            }
            out += c;
        }
        return out;
    }

    void bakeUVChars() {
        //flips Y axis and calculates UV
        for (var pair : chars.entrySet()) {
            var ch = pair.getValue();
            ch.u1 = (ch.u + ch.width) / scaleW;
            ch.v1 = (ch.v + ch.height) / scaleH;

            ch.u /= scaleW;
            ch.v /= scaleH;

            ch.v1 = 1 - ch.v1;
            ch.v = 1 - ch.v;
            var foo = ch.v;
            ch.v = ch.v1;
            ch.v1 = foo;

            ch.yoffset = lineHeight - ch.yoffset - ch.height;
        }
    }

    public static class Lib {

        private static Map<String, Font> s_fonts;

        public static void registerFont(String id, Font font) {
            s_fonts.put(id, font);
        }

        public static Font getFont(String id) {
            return s_fonts.get(id);
        }
    }

    public static class Parser {

        public static Font parse(String filePath) {
            var font = new Font();
            int charCount = 0;
            var lines = IOUtils.readFileLines(ND_RESLOC(filePath));
            if (lines.isEmpty()) {
                ND_WARN("Invalid font path: {} / {}", filePath, ND_RESLOC(filePath));
                return null;
            }

            for (var line : lines) {
                if (line.isEmpty())
                    continue;
                var words = line.split("[\\n\\t ]");

                var title = words[0];

                if (Objects.equals(title, "info")) {
                    for (int i = 1; i < words.length; ++i) {
                        var var = words[i];
                        var arg = var.substring(var.indexOf('=') + 1);
                        if (var.startsWith("face="))
                            font.face = arg.substring(1, arg.length() - 2);
                        else if (var.startsWith("size="))
                            font.size = Integer.parseInt(arg);
                        else if (var.startsWith("bold="))
                            font.bold = Integer.parseInt(arg) == 1;
                        else if (var.startsWith("italic="))
                            font.italic = Integer.parseInt(arg) == 1;
                        else if (var.startsWith("padding=")) {
                            var data = arg.split(",");
                            ASSERT(data.length == 4, "Invalid font file");
                            for (int o = 0; o < 4; ++o)
                                font.padding[o] = Integer.parseInt(data[o]);
                        } else if (var.startsWith("spacing=")) {
                            var data = arg.split(",");
                            ASSERT(data.length == 2, "Invalid font file");
                            for (int o = 0; o < 2; ++o)
                                font.spacing[o] = Integer.parseInt(data[o]);
                        }
                    }
                } else if (title.equals("common")) {
                    for (int i = 1; i < words.length; ++i) {
                        var var = words[i];
                        var arg = var.substring(var.indexOf('=') + 1);

                        if (var.startsWith("lineHeight="))
                            font.lineHeight = Integer.parseInt(arg);
                        else if (var.startsWith("base="))
                            font.base = Integer.parseInt(arg);
                        else if (var.startsWith("scaleW="))
                            font.scaleW = Integer.parseInt(arg);
                        else if (var.startsWith("scaleH="))
                            font.scaleH = Integer.parseInt(arg);
                    }
                } else if (title.equals("page")) {
                    for (int i = 1; i < words.length; ++i) {
                        var var = words[i];
                        var arg = var.substring(var.indexOf('=') + 1);

                        if (var.startsWith("file="))
                            font.texturePath = arg.substring(1, arg.length() - 1);
                    }
                } else if (title.equals("chars")) {
                    for (int i = 1; i < words.length; ++i) {
                        var var = words[i];
                        var arg = var.substring(var.indexOf('=') + 1);

                        if (var.startsWith("count="))
                            charCount = Integer.parseInt(arg);
                    }
                } else if (title.equals("char")) {
                    Charac ch = new Charac();
                    for (int i = 1; i < words.length; ++i) {
                        var var = words[i];
                        var arg = var.substring(var.indexOf('=') + 1);

                        if (var.startsWith("id="))
                            ch.id = Integer.parseInt(arg);
                        else if (var.startsWith("x="))
                            ch.u = Integer.parseInt(arg);
                        else if (var.startsWith("y="))
                            ch.v = Integer.parseInt(arg);
                        else if (var.startsWith("width="))
                            ch.width = Integer.parseInt(arg);
                        else if (var.startsWith("height="))
                            ch.height = Integer.parseInt(arg);
                        else if (var.startsWith("xoffset="))
                            ch.xoffset = Integer.parseInt(arg);
                        else if (var.startsWith("yoffset="))
                            ch.yoffset = Integer.parseInt(arg);
                        else if (var.startsWith("xadvance="))
                            ch.xadvance = Integer.parseInt(arg);
                    }
                    //font.chars.push_back(ch);
                    font.chars.put(ch.id, ch);
                } else if (title.equals("kernings")) {
                    for (int i = 1; i < words.length; ++i) {
                        var var = words[i];
                        var arg = var.substring(var.indexOf('=') + 1);

                        //if (var.startsWith("count="))
                        // font.kerning.reserve(Integer.parseInt (arg));
                    }
                } else if (title.equals("kerning")) {
                    int first = 0, second = 0, amount = 0;
                    for (int i = 1; i < words.length; ++i) {
                        var var = words[i];
                        var arg = var.substring(var.indexOf('=') + 1);

                        if (var.startsWith("first="))
                            first = Integer.parseInt(arg);
                        else if (var.startsWith("second"))
                            second = Integer.parseInt(arg);
                        else if (var.startsWith("amount="))
                            amount = Integer.parseInt(arg);
                    }
                    font.setKerning(first, second, amount);
                }
            }
            if (font.chars.size() != charCount)
                ND_WARN("Strange font file {}", ND_RESLOC(filePath));
            font.bakeUVChars();
            return font;
        }
    }
}


