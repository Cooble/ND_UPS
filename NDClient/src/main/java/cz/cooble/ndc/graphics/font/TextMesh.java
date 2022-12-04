package cz.cooble.ndc.graphics.font;

import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.Arrays;


import static org.joml.Math.max;
import static org.joml.Math.min;

public class TextMesh {

    public static class Vertex {

        public Vector2f pos;
        public Vector2f uv;

        public int color;
        public int borderColor;

        Vertex(Vector2f pos, Vector2f uv, int color, int borderColor) {
            this.pos = pos;
            this.uv = uv;
            this.color = color;
            this.borderColor = borderColor;
        }

        Vertex(float x, float y, float u, float v, int color, int borderColor) {
            this.pos = new Vector2f(x, y);
            this.uv = new Vector2f(u, v);
            this.color = color;
            this.borderColor = borderColor;
        }
    }

    public static class CursorProp {
        public int cursorPos = -1;
        public char cursorCharacter;

        public Vector4f positions;
        public Vector4f uvs;

        public boolean isEnabled() {
            return cursorPos != -1;
        }
    }

    static final int VERTEX_BYTES = (2 + 2 + 1 + 1) * 4;
    static final int CHARM_BYTES = VERTEX_BYTES * 4;

    public static class CharM {public Vertex[] v = new Vertex[4];}

    CharM[] src;
    // usually stores hash of the string
    int id;

    //how many chars are active
    //range from 0 to getCharCount()
    public int currentCharCount = 0;

    public TextMesh(int charCount) {
        src = new CharM[charCount];
    }

    public int getMaxCharCount() {
        return src.length;
    }

    public int getMaxByteSize() {
        return src.length * CHARM_BYTES;
    }

    public CharM[] getSrc() {
        return src;
    }

    public void clear() {
        Arrays.fill(src, null);
    }

    public int getVertexCount() {
        return currentCharCount * 4;
    }

    public void setChar(int index, float x, float y, float x1, float y1, int color, int borderColor, Font.Charac ch) {
        var che = src[index];

        che.v[0] = new Vertex(x, y, ch.u, ch.v, color, borderColor);
        che.v[1] = new Vertex(x1, y, ch.u1, ch.v, color, borderColor);
        che.v[2] = new Vertex(x1, y1, ch.u1, ch.v1, color, borderColor);
        che.v[3] = new Vertex(x, y1, ch.u, ch.v1, color, borderColor);
    }

    public void setChar(int index, float x, float y, float x1, float y1, float u, float v, float u1, float v1,
                        int color, int borderColor, Font.Charac ch) {
        var che = new CharM();
        src[index]=che;
        che.v[0] = new Vertex(x, y, ch.u + u, ch.v + v, color, borderColor);
        che.v[1] = new Vertex(x1, y, ch.u1 + u1, ch.v + v, color, borderColor);
        che.v[2] = new Vertex(x1, y1, ch.u1 + u1, ch.v1 + v1, color, borderColor);
        che.v[3] = new Vertex(x, y1, ch.u + u, ch.v1 + v1, color, borderColor);
    }

    public void resize(int size) {
        src = new CharM[size];
        currentCharCount = 0;
    }

    public void reserve(int size) {
        if (getMaxCharCount() < size)
            resize(size);
    }


    static class clipper {
        float minX, minY, maxX, maxY;

        clipper(float minX, float minY, float maxX, float maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }

    public static void setCursorData(float currentX, float yLoc, float pixelRat, clipper clip, Font font, CursorProp cursor) {
        float x0 = currentX + font.getChar(cursor.cursorCharacter).xoffset;

        var cc = font.getChar(cursor.cursorCharacter);
        x0 -= cc.width;

        float y0 = yLoc + cc.yoffset;

        float x1 = x0 + cc.width;
        float y1 = y0 + cc.height;

        float fx0 = max(clip.minX, x0);
        float u0 = pixelRat * (fx0 - x0);

        float fx1 = min(clip.maxX, x1);
        float u1 = -pixelRat * (x1 - fx1);

        float fy0 = max(clip.minY, y0);
        float v0 = pixelRat * (fy0 - y0);

        float fy1 = min(clip.maxY, y1);
        float v1 = -pixelRat * (y1 - fy1);
        cursor.positions = new Vector4f(fx0, fy0, fx1, fy1);
        cursor.uvs = new Vector4f(u0, v0, u1, v1);
    }
}
