package cz.cooble.ndc.graphics.font;

import org.joml.Vector4f;
import org.joml.Vector4i;

import java.util.ArrayList;
import java.util.List;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.Asserter.ND_WARN;
import static cz.cooble.ndc.graphics.font.TextMesh.setCursorData;
import static org.joml.Math.max;
import static org.joml.Math.min;

public class TextBuilder {

    public static final int ALIGN_RIGHT = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_CENTER = 2;

    public static int getMaxLength(List<String> lines,Font font){
        int out =0;
        for(var e:lines)
            out = Math.max(font.getTextWidth(e),out);
        return out;
    }
    public static boolean buildMesh(List<String> lines, Font font, TextMesh mesh, int alignment, Vector4i clipRect, TextMesh.CursorProp cursor) {
        if(clipRect==null)
            clipRect = new Vector4i(-100000,-100000,100000,100000);
        TextMesh.clipper clip = new TextMesh.clipper((float) clipRect.x, (float) clipRect.y, (float) clipRect.z, (float) clipRect.w);

        float pixelRat = font.getPixelRatio();


        float yLoc = 0;
        mesh.currentCharCount = 0;
        int currentCharPos = 0;

        float defaultXPos;

        int color = 0xffffff00;
        int borderColor = Font.colorToInt(new Vector4f(0, 0.1f, 0.7f, 1));

        for (var line : lines) {
            float currentX;
            switch (alignment) {
                case ALIGN_CENTER:
                    currentX = -(float) font.getTextWidth(line) / 2;
                    break;
                case ALIGN_LEFT:
                    currentX = 0;
                    break;
                case ALIGN_RIGHT:
                    currentX = -(float) font.getTextWidth(line);
                    break;
                default:
                    currentX = 0;
                    ASSERT(false, "Invalid ALIGNMENT");
                    break;
            }
            int lastC = 0;
            int ignoreColorChar = 0;
            int currentLineIndex = 0;
            for (int c : line.toCharArray()) {
                boolean isBorderPrefix = false;
                boolean isIgnorePrefix = false;
                if (currentLineIndex != 0) {
                    isBorderPrefix = line.charAt(currentLineIndex - 1) == Font.BORDER_PREFIX;
                    isIgnorePrefix = line.charAt(currentLineIndex - 1) == Font.IGNORE_PREFIX;
                }

                if (c == '&') {
                    var col = Font.tryEntityToColor(line.substring(currentLineIndex));
                    if (col != null && !isIgnorePrefix) {
                        if (!isBorderPrefix)
                            color = col;
                        else
                            borderColor = col;
                        ignoreColorChar = 2;
                    }
                } else if (c == '#') {
                    var col = Font.tryEntityToColor(line.substring(currentLineIndex));
                    if (col != null && !isIgnorePrefix) {
                        if (!isBorderPrefix)
                            color = col;
                        else
                            borderColor = col;
                        ignoreColorChar = 7;
                    }
                } else if ((c == Font.BORDER_PREFIX || c == Font.IGNORE_PREFIX) && currentLineIndex + 1 < line.length())
                //check if in future the color is valid
                {
                    var col = Font.tryEntityToColor(line.substring(currentLineIndex + 1));
                    if (col != null) {
                        ignoreColorChar = 1;
                    }
                }

                currentLineIndex++;
                if (ignoreColorChar != 0) {
                    ignoreColorChar--;
                    currentCharPos++;
                    continue;
                }
                float kerning = font.getKerning(lastC, c);
                if (mesh.currentCharCount == mesh.getMaxCharCount()) {
                    ND_WARN("TextMesh too small");
                    return false;
                }
                var cc = font.getChar(c);

                float x0 = currentX + cc.xoffset + kerning;
                float y0 = yLoc + cc.yoffset;

                float x1 = x0 + cc.width;
                float y1 = y0 + cc.height;

                if (!(x1 < clip.minX || x0 > clip.maxX || y1 < clip.minY || y0 > clip.maxY)) {
                    float fx0 = max(clip.minX, x0);
                    float u0 = pixelRat * (fx0 - x0);

                    float fx1 = min(clip.maxX, x1);
                    float u1 = -pixelRat * (x1 - fx1);

                    float fy0 = max(clip.minY, y0);
                    float v0 = pixelRat * (fy0 - y0);

                    float fy1 = min(clip.maxY, y1);
                    float v1 = -pixelRat * (y1 - fy1);

                    mesh.setChar(mesh.currentCharCount++, fx0, fy0, fx1, fy1, u0, v0, u1, v1, color, borderColor, cc);

                    if (cursor != null && currentCharPos == cursor.cursorPos) {
                        setCursorData(currentX, yLoc, pixelRat, clip, font, cursor);
                        cursor = null;
                    }
                }

                currentX += cc.xadvance + font.xSpace + kerning;
                currentCharPos++;
                lastC = c;
            }
            //on the edge of line
            if (cursor != null && currentCharPos == cursor.cursorPos) {
                setCursorData(currentX, yLoc, pixelRat, clip, font, cursor);
                cursor = null;
            }
            yLoc -= font.lineHeight + font.ySpace;
        }

        return true;
    }


    public static List<String> convertToLines(String text){
        ArrayList<String> out  = new ArrayList<>();
        out.add(text);
        return out;
    }
    public static List<String> convertToLines(String text, Font font, int maxLineWidth) {
        List<String> lines = new ArrayList<>();

        var separatedLines = Font.removeColorEntities(text).split("\\n");

        for (var sepLine : separatedLines) {
            int currentWidth = 0;
            String currentLine = "";
            var words = sepLine.split("[\\n\\t ]");

            for (var word : words) {
                var wordSize = font.getTextWidth(word);
                if (currentWidth + wordSize <= maxLineWidth) {
                    currentLine += (currentLine.length() != 0 ? " " : "") + word;
                } else {
                    lines.add(currentLine);
                    currentWidth = wordSize;
                    currentLine = word;
                }
            }
            if (!currentLine.isEmpty())
                lines.add(currentLine);
        }
        return lines;
    }
}
