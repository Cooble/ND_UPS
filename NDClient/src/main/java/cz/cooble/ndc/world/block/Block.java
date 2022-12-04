package cz.cooble.ndc.world.block;

import cz.cooble.ndc.physics.Polygon;
import cz.cooble.ndc.world.BlockStruct;
import cz.cooble.ndc.world.World;
import cz.cooble.ndc.world.WorldEntity;

import static cz.cooble.ndc.core.Utils.*;
import static cz.cooble.ndc.world.BlockStruct.*;

public class Block {
    public static final int BLOCK_FLAG_HAS_ITEM_VERSION = 0;

    // varmatically get texture uv offset by adding meta to x coordinate of texture uv
    public static final int BLOCK_FLAG_HAS_METATEXTURES_IN_ROW = 1;
    //block needs wall behind it (e.g Painting)
    public static final int BLOCK_FLAG_NEEDS_WALL = 2;
    //block cannot float in the air
    public static final int BLOCK_FLAG_CANNOT_FLOAT = 3;

    public static final int BLOCK_FLAG_HAS_BIG_TEXTURE = 4;

    //on block can be placed candle or something
    public static final int BLOCK_FLAG_SOLID = 5;

    //can be replaced by another block for example flowers
    public static final int BLOCK_FLAG_REPLACEABLE = 6;

    //can be replaced by another block for example flowers
    public static final int BLOCK_FLAG_TRANSPARENT = 7;

    int m_id;
    String m_string_id;

    int m_flags = 0;
    float m_hardness = 1;
    int m_tier = 1;
    int m_required = 0;

    /**
     * when one block has for example more flower species this number tells us how many
     * used by itemblock
     */
    int m_max_metadata = 0;

    //half_int offset in block texture atlas
    int m_texture_pos;
    //array[BLOCK_STATE]=TEXTURE_OFFSET
    int[] m_corner_translate_array = BlockDatas.BLOCK_CORNERS_DIRT;

    //if yes the texturepos will be added based on location of block in chunk
    //boolean m_has_big_texture;

    Polygon[] m_collision_box = BlockDatas.BLOCK_BOUNDS_DEFAULT;
    int m_collision_box_size = 3;

    int m_block_connect_group;

    int getID() {
        return m_id;
    }

    int getConnectGroup() {
        return m_block_connect_group;
    }

    int getMaxMetadata() {
        return m_max_metadata;
    }


    public boolean hasCollisionBox() {
        return BOOL(m_collision_box_size);
    }

    public String getStringID() {
        return m_string_id;
    }

    boolean isInConnectGroup(int groups) {return BOOL(groups & m_block_connect_group);}
    //they have group in common 

    //basic flags
    public boolean hasItemVersion() {return BOOL(m_flags & 1 << BLOCK_FLAG_HAS_ITEM_VERSION);}

    public boolean hasMetaTexturesInRow() {return BOOL(m_flags & 1 << BLOCK_FLAG_HAS_METATEXTURES_IN_ROW);}

    public boolean needsWall() {return BOOL(m_flags & 1 << BLOCK_FLAG_NEEDS_WALL);}

    public boolean cannotFloat() {return BOOL(m_flags & 1 << BLOCK_FLAG_CANNOT_FLOAT);}

    public boolean hasBigTexture() {return BOOL(m_flags & 1 << BLOCK_FLAG_HAS_BIG_TEXTURE);}

    public boolean isSolid() {return BOOL(m_flags & 1 << BLOCK_FLAG_SOLID);}

    public boolean isReplaceable() {return BOOL(m_flags & 1 << BLOCK_FLAG_REPLACEABLE);}

    public boolean hasFlag(int flag) {return BOOL(m_flags & 1 << flag);}

    public void setFlag(int flag, boolean value) {
        if (value)
            m_flags |= 1 << flag;
        else
            m_flags &= ~(1 << flag);
    }

    public void setFlag(int flag) {
       setFlag(flag,true);
    }

    public Block(int id, String name) {
        this.m_id = id;
        this.m_string_id = name;

        setFlag(BLOCK_FLAG_HAS_ITEM_VERSION, true);
        setFlag(BLOCK_FLAG_SOLID, true);
    }

    public Polygon getCollisionBox(int x, int y, BlockStruct b) {
        var i = m_collision_box_size >= 3 ? 1 : 0;
        var ee = BLOCK_STATE_PURE_MASK & b.block_corner;
        if (ee == BLOCK_STATE_CORNER_UP_LEFT) {
            return m_collision_box[1 * i];
        } else if (ee == BLOCK_STATE_CORNER_UP_RIGHT) {
            return m_collision_box[2 * i];
        } else if (ee == BLOCK_STATE_CORNER_DOWN_LEFT) {
            return m_collision_box[3 * i];
        } else if (ee == BLOCK_STATE_CORNER_DOWN_RIGHT) {
            return m_collision_box[4 * i];
        } else if (ee == BLOCK_STATE_LINE_UP) {
            return m_collision_box[5 * i * ((b.block_corner & BLOCK_STATE_HAMMER) != 0 ? 1 : 0)];//needs to be hammer to work
        } else if (ee == BLOCK_STATE_LINE_DOWN) {
            return m_collision_box[6 * i * ((b.block_corner & BLOCK_STATE_HAMMER) != 0 ? 1 : 0)];//needs to be hammer to work
        }
        return m_collision_box[0];
    }

    void onTextureLoaded(BlockTextureAtlas atlas) {
        m_texture_pos = atlas.getTexture("block/" + getStringID());
    }

    public int getTextureOffset(int x, int y, BlockStruct b) {
        if (!hasBigTexture())
            return m_texture_pos;

        var tex_pos = half_int_object(m_texture_pos);

        return half_int(tex_pos.x + (x & 3), tex_pos.y + (y & 3));
    }

    public int getCornerOffset(int x, int y, BlockStruct b) {
        var index = b.block_corner;
        return m_corner_translate_array != null ? m_corner_translate_array[index] : BLOCK_STATE_FULL;
    }

    boolean isInGroup(World w, int x, int y, int group) {
        var b = w.getBlock(x, y);
        if (b != null) {
            var bl = BlockRegistry.getBlock(b.block_id);
            return bl.isInConnectGroup(group) || bl.getID() == m_id;
        }
        return false;
    }

    public boolean onNeighborBlockChange(World world, int x, int y) {
        int mask = 0;
        mask |= INT(!isInGroup(world, x, y + 1, m_block_connect_group)) << 0;
        mask |= INT(!isInGroup(world, x - 1, y, m_block_connect_group)) << 1;
        mask |= INT(!isInGroup(world, x, y - 1, m_block_connect_group)) << 2;
        mask |= INT(!isInGroup(world, x + 1, y, m_block_connect_group)) << 3;


        BlockStruct block = world.getBlock(x, y);
        int lastCorner = block.block_corner;
        block.block_corner = mask | (lastCorner & ~BLOCK_STATE_PURE_MASK);
        return lastCorner != block.block_corner; //we have a change (or not)
    }

    public void onBlockPlaced(World w, WorldEntity e, int x, int y, BlockStruct b) {

    }

    public void onBlockDestroyed(World w, WorldEntity e, int x, int y, BlockStruct b) {

    }

    void onBlockClicked(World w, WorldEntity e, int x, int y, BlockStruct b) {

    }

    boolean canBePlaced(World w, int x, int y) {
        // can be hanged only on wall
        if (needsWall() && w.getBlock(x, y).isWallFree())
            return false;

        // block is already occupied with another block
        if (!w.getBlockInstance(x, y).isReplaceable())
            return false;

        // can be put only on ground
        if (cannotFloat() && !w.getBlockInstance(x, y - 1).isSolid())
            return false;

        return true;
    }


}
