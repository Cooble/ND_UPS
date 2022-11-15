package cz.cooble.ndc.graphics;

import org.joml.Vector2f;

import static cz.cooble.ndc.core.Utils.half_int;

public class Camera {

    Vector2f position = new Vector2f();
    int chunk_radius_half_int = half_int(1,1);


    public int getChunkRadius() {
        return chunk_radius_half_int;
    }

    public void setChunkRadius(int chunk_radius_half_int) {
        this.chunk_radius_half_int = chunk_radius_half_int;
    }

    public void setPosition(Vector2f position) {
        this.position = position;
    }

    public Vector2f getPosition() {
        return position;
    }
}
