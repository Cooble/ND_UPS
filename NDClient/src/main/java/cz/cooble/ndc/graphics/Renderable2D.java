package cz.cooble.ndc.graphics;

import org.joml.Vector2f;
import org.joml.Vector3f;

public interface Renderable2D {



    Vector3f getPosition();

    Vector2f getSize();

    Vector2f[] getUV();

    Texture getTexture();

    static Vector2f[] buildUV(Vector2f pos, Vector2f size) {
        return buildUV(pos, size, false, false, false);
    }

    static Vector2f[] elementary() {
        return buildUV(new Vector2f(0,0),new Vector2f(1,1));
    }

    static Vector2f[] buildUV(Vector2f pos, Vector2f size, boolean horizontalFlip, boolean verticalFlip, boolean rotate90) {
        Vector2f pos0 = new Vector2f(pos);
        Vector2f pos1 = new Vector2f(pos).add(size);
        if (horizontalFlip) {
            float x = pos0.x;
            pos0.x = pos1.x;
            pos1.x = x;
        }
        if (verticalFlip) {
            float y = pos0.y;
            pos0.y = pos1.y;
            pos1.y = y;
        }

        Vector2f[] out = new Vector2f[4];

        out[0] = pos0;
        out[1] = new Vector2f(pos1.x, pos0.y);
        out[2] = pos1;
        out[3] = new Vector2f(pos0.x, pos1.y);

        if (rotate90) {
            var last = out[0];
            out[0] = out[1];
            out[1] = out[2];
            out[2] = out[3];
            out[3] = last;
        }

        return out;
    }
}
