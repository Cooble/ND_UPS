package cz.cooble.ndc.graphics;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Sprite implements Renderable2D,BatchRenderable2D {

    SpriteSheetResource m_resource;
    Vector2f[] m_uv_quad;
    Vector3f m_position;
    Vector2f m_size;
    boolean m_enabled;

    public Sprite(SpriteSheetResource r) {
        this.m_resource = r;
        this.m_uv_quad = Renderable2D.buildUV(new Vector2f(0, 0), new Vector2f(1, 1));
        this.m_position = new Vector3f(0, 0, 0);
        this.m_size = new Vector2f(1, 1);
        this.m_enabled = true;
    }

    public void setPosition(Vector3f pos) {m_position = pos;}

    public void setSize(Vector2f size) {m_size = size;}

    public Texture getTexture() {return m_resource.getTexture();}

    public Vector3f getPosition() {return m_position;}

    public Vector2f getSize() {return m_size;}

    public boolean isEnabled() {return m_enabled;}

    public void setEnabled(boolean enabled) {m_enabled = enabled;}

    public Vector2f[] getUV() {return m_uv_quad;}

    public void setSpriteIndex(int u, int v, boolean horizontalFlip, boolean verticalFlip, boolean rotate90) {
        float xPiece = 1.f / m_resource.getIconsWidth();
        float yPiece = 1.f / m_resource.getIconsHeight();

        m_uv_quad = Renderable2D.buildUV(new Vector2f(xPiece * u, yPiece * v), new Vector2f(xPiece, yPiece), horizontalFlip, verticalFlip, rotate90);
    }

    public void render(BatchRenderer2D r) {
        if (m_enabled)
            r.submit(this);
    }
}
