package cz.cooble.ndc;

import cz.cooble.ndc.graphics.Renderable2D;
import cz.cooble.ndc.graphics.Texture;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class ExampleQuad implements Renderable2D {
    private Vector3f position;
    private Vector2f size;
    private Vector2f[] uv;
    private Texture texture;

    public ExampleQuad(Vector3f position, Vector2f size, Vector2f[] uv, Texture texture) {
        this.position = position;
        this.size = size;
        this.uv = uv;
        this.texture = texture;
    }


    @Override
    public Vector3f getPosition() {
        return position;
    }

    @Override
    public Vector2f getSize() {
        return size;
    }

    @Override
    public Vector2f[] getUV() {
        return uv;
    }

    @Override
    public Texture getTexture() {
        return texture;
    }
}
