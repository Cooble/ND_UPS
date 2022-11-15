package cz.cooble.ndc.world;

import java.util.Arrays;

import static cz.cooble.ndc.Asserter.ASSERT;

public class BlockStruct {

    private static int BIT(int i) {
        return 1 << i;
    }

    //STATES=========================================================
//is air counterclockwise start from up(=lsb) (up left down right)
    public static final int BLOCK_STATE_FULL = 0;
    //		  ______________
//		 <              >
//		 >			       <
//		 <\/\/\/\/\/\/\/>
//
    public static final int BLOCK_STATE_LINE_UP = BIT(0);
    public static final int BLOCK_STATE_LINE_DOWN = BIT(2);
    public static final int BLOCK_STATE_LINE_LEFT = BIT(1);
    public static final int BLOCK_STATE_LINE_RIGHT = BIT(3);
    public static final int BLOCK_STATE_VARIABLE = BIT(4);
    public static final int BLOCK_STATE_CRACKED = BIT(5);
    public static final int BLOCK_STATE_HAMMER = BIT(6);

    //all bits except for cracked
    public static final int BLOCK_STATE_PURE_MASK = BLOCK_STATE_LINE_UP | BLOCK_STATE_LINE_DOWN | BLOCK_STATE_LINE_LEFT |
            BLOCK_STATE_LINE_RIGHT;


    //     ____
//    |      \     
//    |		    \
//    |__________|
//
    public static final int BLOCK_STATE_CORNER_UP_LEFT = BLOCK_STATE_LINE_UP | BLOCK_STATE_LINE_LEFT;
    public static final int BLOCK_STATE_CORNER_UP_RIGHT = BLOCK_STATE_LINE_UP | BLOCK_STATE_LINE_RIGHT;
    public static final int BLOCK_STATE_CORNER_DOWN_LEFT = BLOCK_STATE_LINE_DOWN | BLOCK_STATE_LINE_LEFT;
    public static final int BLOCK_STATE_CORNER_DOWN_RIGHT = BLOCK_STATE_LINE_DOWN | BLOCK_STATE_LINE_RIGHT;

    public static final int BLOCK_STATE_BIT = BLOCK_STATE_CORNER_UP_LEFT | BLOCK_STATE_CORNER_DOWN_RIGHT;
    //		  ______________
//		 <              >
//		 >			       <
//		 <______________>
//
    public static final int BLOCK_STATE_LINE_HORIZONTAL = BLOCK_STATE_LINE_UP | BLOCK_STATE_LINE_DOWN;
    public static final int BLOCK_STATE_LINE_VERTICAL = BLOCK_STATE_LINE_LEFT | BLOCK_STATE_LINE_RIGHT;

    //______________
//              \
//				    |
//______________/
//
    public static final int BLOCK_STATE_LINE_END_UP = BLOCK_STATE_LINE_VERTICAL | BLOCK_STATE_LINE_UP;
    public static final int BLOCK_STATE_LINE_END_LEFT = BLOCK_STATE_LINE_HORIZONTAL | BLOCK_STATE_LINE_LEFT;
    public static final int BLOCK_STATE_LINE_END_DOWN = BLOCK_STATE_LINE_VERTICAL | BLOCK_STATE_LINE_DOWN;
    public static final int BLOCK_STATE_LINE_END_RIGHT = BLOCK_STATE_LINE_HORIZONTAL | BLOCK_STATE_LINE_RIGHT;


    public int block_id;
    public int block_metadata;
    public int block_corner;
    public int[] wall_id = new int[4];
    public byte[] wall_corner = new byte[4];

    public BlockStruct() {
        this(0);
    }

    public BlockStruct(int block_id) {
        this.block_id = block_id;
    }

    public int wallID() {
        ASSERT(isWallFullyOccupied(), "calling wallid on shared block");
        return wall_id[0];
    }

    public boolean isAir() {
        return block_id == 0;
    }

    //block is either full air or shared
    public boolean isWallFree() {
        return wall_corner[0] != BLOCK_STATE_FULL ||
                wall_corner[1] != BLOCK_STATE_FULL ||
                wall_corner[2] != BLOCK_STATE_FULL ||
                wall_corner[3] != BLOCK_STATE_FULL || wall_id[0] == 0;
    }

    public void setWall(int id) {
        for (int i = 0; i < 4; ++i) {
            wall_id[i] = id;
            wall_corner[i] = BLOCK_STATE_FULL;
        }
    }

    //block is either full block or full air
    // i.e is not partially filled
    public boolean isWallFullyOccupied() {
        for (int i = 0; i < 4; i++) {
            if (wall_corner[i] != 0)
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        int[] o = new int[4];
        for (int i = 0; i < wall_corner.length; i++) {
            o[i] = wall_corner[i];
        }
        return "{" +
                "b_id=" + block_id +
                ", b_co=" + block_corner +
                ", w_id=" + Arrays.toString(wall_id) +
                ", w_co=" + Arrays.toString(o) +
                '}';
    }
}
