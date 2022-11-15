package cz.cooble.ndc.graphics;

public class SpriteSheetResource {
    Texture m_texture;
    int m_width_icons;
    int m_height_icons;

    //width - means how many subimages texture contains in one row
    //height - means how many subimages texture contains in one column
    public SpriteSheetResource(Texture t, int width, int height) {
        this.m_texture = t;
        this.m_width_icons = width;
        this.m_height_icons = height;
    }

    public Texture getTexture() {return m_texture;}

    public int getIconsWidth() {return m_width_icons;}

    public int getIconsHeight() {return m_height_icons;}
}
