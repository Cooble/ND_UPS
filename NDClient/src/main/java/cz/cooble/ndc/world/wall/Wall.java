package cz.cooble.ndc.world.wall;

import cz.cooble.ndc.world.BlockStruct;
import cz.cooble.ndc.world.World;
import cz.cooble.ndc.world.block.BlockDatas;
import cz.cooble.ndc.world.block.BlockRegistry;
import cz.cooble.ndc.world.block.BlockTextureAtlas;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.core.Utils.*;
import static cz.cooble.ndc.world.BlockStruct.*;
import static cz.cooble.ndc.world.block.Block.*;

public class Wall {
    int m_id;
    String m_string_id;
    int m_flags = 0;

    //offset in block texture atlas
    int m_texture_pos;
    //array[BLOCK_STATE]=TEXTURE_OFFSET
    int[] m_corner_translate_array;

    public int getID() {return m_id;}

    //flags

    public void setFlag(int flag, boolean value) {
        if (value)
            m_flags |= 1 << flag;
        else
            m_flags &= ~(1 << flag);
    }

    public boolean hasItemVersion() {return BOOL(m_flags & 1 << BLOCK_FLAG_HAS_ITEM_VERSION);}

    public boolean isReplaceable() {return BOOL(m_flags & 1 << BLOCK_FLAG_REPLACEABLE);}

    public boolean isTransparent() {return BOOL(m_flags & 1 << BLOCK_FLAG_TRANSPARENT);}

    public String getStringID() {return m_string_id;}


    Wall(String name, int id) {
        this.m_string_id = name;
        this.m_id = id;
        m_corner_translate_array = BlockDatas.WALL_CORNERS_DIRT;

        setFlag(BLOCK_FLAG_HAS_ITEM_VERSION, true);
        setFlag(BLOCK_FLAG_SOLID, true);
    }


    public void onTextureLoaded(BlockTextureAtlas atlas) {
        m_texture_pos = atlas.getTexture("wall/" + getStringID());
    }

    public int getTextureOffset(int wx, int wy, BlockStruct e) {
        return half_int(getX(m_texture_pos) * 2 + (wx & 7), getY(m_texture_pos) * 2 + (wy & 7));
    }

    public int getCornerOffset(int wx, int wy, BlockStruct b) {
        ASSERT(m_corner_translate_array != null, " m_corner_translate_array  cannot be null");
        int i = m_corner_translate_array[b.wall_corner[(wy & 1) * 2 + (wx & 1)]];

        return plus_half_int(half_int(((wx & 1) + (wy & 1) & 1) * 3, 0), i);
    }

    public static boolean isWallFree(World w, int x, int y) {
        var b = w.getBlock(x, y);
        return b != null && b.isWallFree(); //if we dont know we will assume is occupied
    }

    public void onNeighbourWallChange(World w, int x, int y) {
        var p = w.getBlock(x, y);
        ASSERT(p != null, "fuk");
        //left
        if (isWallFree(w, x - 1, y)) {
            var b = w.getBlock(x - 1, y);
            b.wall_id[1] = m_id;
            b.wall_id[3] = m_id;
            b.wall_corner[1] = (byte) BLOCK_STATE_LINE_LEFT;
            b.wall_corner[3] = (byte) BLOCK_STATE_LINE_LEFT;
        }
        //right
        if (isWallFree(w, x + 1, y)) {
            var b = w.getBlock(x + 1, y);
            b.wall_id[0] = m_id;
            b.wall_id[2] = m_id;
            b.wall_corner[0] = (byte) BLOCK_STATE_LINE_RIGHT;
            b.wall_corner[2] = (byte) BLOCK_STATE_LINE_RIGHT;
        }
        //up
        if (isWallFree(w, x, y + 1)) {
            var b = w.getBlock(x, y + 1);
            b.wall_id[0] = m_id;
            b.wall_id[1] = m_id;
            b.wall_corner[0] = (byte) BLOCK_STATE_LINE_UP;
            b.wall_corner[1] = (byte) BLOCK_STATE_LINE_UP;
        }
        //down
        if (isWallFree(w, x, y - 1)) {
            var b = w.getBlock(x, y - 1);
            b.wall_id[2] = m_id;
            b.wall_id[3] = m_id;
            b.wall_corner[2] = (byte) BLOCK_STATE_LINE_DOWN;
            b.wall_corner[3] = (byte) BLOCK_STATE_LINE_DOWN;
        }
        //corner left up

        if (isWallFree(w, x - 1, y + 1)
                && isWallFree(w, x - 1, y)
                && isWallFree(w, x, y + 1)) {
            var b = w.getBlock(x - 1, y + 1);
            b.wall_id[1] = m_id;
            b.wall_corner[1] = (byte) BLOCK_STATE_CORNER_UP_LEFT;
        }

        //corner left down

        if (isWallFree(w, x - 1, y - 1)
                && isWallFree(w, x - 1, y)
                && isWallFree(w, x, y - 1)) {
            var b = w.getBlock(x - 1, y - 1);
            b.wall_id[3] = m_id;
            b.wall_corner[3] = (byte) BLOCK_STATE_CORNER_DOWN_LEFT;
        }

        //corner right up

        if (isWallFree(w, x + 1, y + 1)
                && isWallFree(w, x + 1, y)
                && isWallFree(w, x, y + 1)) {
            var b = w.getBlock(x + 1, y + 1);
            b.wall_id[0] = m_id;
            b.wall_corner[0] = (byte) BLOCK_STATE_CORNER_UP_RIGHT;
        }

        //corner right down

        if (isWallFree(w, x + 1, y - 1)
                && isWallFree(w, x + 1, y)
                && isWallFree(w, x, y - 1)) {
            var b = w.getBlock(x + 1, y - 1);
            b.wall_id[2] = m_id;
            b.wall_corner[2] = (byte) BLOCK_STATE_CORNER_DOWN_RIGHT;
        }
    }

    boolean canBePlaced(World w, int x, int y) {
        var block = w.getBlock(x, y);
        return block.isWallFree() || BlockRegistry.getWall(block.wallID()).isReplaceable();
    }
}
