package cz.cooble.ndc.world.block;

import cz.cooble.ndc.physics.Polygon;
import cz.cooble.ndc.physics.Rect;
import org.joml.Vector2f;

import static cz.cooble.ndc.core.Utils.half_int;

public class BlockDatas {
    public static final Polygon BLOCK_BOUNDS_DEFAULT[] = {
            Polygon.toPolygon(Rect.createFromDimensions(0, 0, 1, 1)), //0 BLOCK_STATE_FULL
            new Polygon(new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1)), //1 LEFT_UP_CORNER
            new Polygon(new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(0, 1)), //2 RIGHT_UP_CORNER
            new Polygon(new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1)), //3 LEFT_DOWN_CORNER
            new Polygon(new Vector2f(0, 0), new Vector2f(1, 1), new Vector2f(0, 1)), //4 RIGHT_DOWN_CORNER
            Polygon.toPolygon(Rect.createFromDimensions(0, 0, 1, 0.25f)), //5 UP_LINE
            Polygon.toPolygon(Rect.createFromDimensions(0, 0.75f, 1, 0.25f)), //6 DOWN_LINE
    };
    public static final int BLOCK_GROUP_AIR_BIT = 0;
    public static final int BLOCK_GROUP_DIRT_BIT = 1;
    public static final int BLOCK_GROUP_PLATFORM_BIT = 2;
    public static final int BLOCK_GROUP_ORE_BIT = 3;


//=======================BLOCKSTATE Ids=======================
//00 BLOCK_STATE_FULL
//01 BLOCK_STATE_LINE_UP
//02 BLOCK_STATE_LINE_LEFT
//03 BLOCK_STATE_CORNER_UP_LEFT
//04 BLOCK_STATE_LINE_DOWN
//05 BLOCK_STATE_LINE_HORIZONTAL
//06 BLOCK_STATE_CORNER_DOWN_LEFT
//07 BLOCK_STATE_LINE_END_LEFT
//08 BLOCK_STATE_LINE_RIGHT
//09 BLOCK_STATE_CORNER_UP_RIGHT
//10 BLOCK_STATE_LINE_VERTICAL
//11 BLOCK_STATE_LINE_END_UP
//12 BLOCK_STATE_CORNER_DOWN_RIGHT
//13 BLOCK_STATE_LINE_END_RIGHT
//14 BLOCK_STATE_LINE_END_DOWN
//15 BLOCK_STATE_BIT

//=======================CORNER MAPPING=======================

    public static final int[] BLOCK_CORNERS_DIRT = new int[]{
            //000 first column
            half_int(0, 0), //00 BLOCK_STATE_FULL
            half_int(0, 14), //01 BLOCK_STATE_LINE_UP
            half_int(0, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(0, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(0, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(0, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(0, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(0, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(0, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(0, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(0, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(0, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(0, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(0, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(0, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(0, 15), //15 BLOCK_STATE_BIT

            //001 second column
            half_int(1, 0), //00 BLOCK_STATE_FULL
            half_int(1, 14), //01 BLOCK_STATE_LINE_UP
            half_int(1, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(1, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(1, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(1, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(1, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(1, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(1, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(1, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(1, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(1, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(1, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(1, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(1, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(1, 15), //15 BLOCK_STATE_BIT

            //010 cracks column 1
            half_int(3, 0), //00 BLOCK_STATE_FULL
            half_int(3, 14), //01 BLOCK_STATE_LINE_UP
            half_int(3, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(3, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(3, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(3, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(3, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(3, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(3, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(3, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(3, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(3, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(3, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(3, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(3, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(3, 15), //15 BLOCK_STATE_BIT

            //011 cracks column 2
            half_int(4, 0), //00 BLOCK_STATE_FULL
            half_int(4, 14), //01 BLOCK_STATE_LINE_UP
            half_int(4, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(4, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(4, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(4, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(4, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(4, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(4, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(4, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(4, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(4, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(4, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(4, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(4, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(4, 15), //15 BLOCK_STATE_BIT

            //100 hammer column
            half_int(6, 0), //00 BLOCK_STATE_FULL
            half_int(6, 14), //01 BLOCK_STATE_LINE_UP
            half_int(6, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(6, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(6, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(6, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(6, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(6, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(6, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(6, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(6, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(6, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(6, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(6, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(6, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(6, 15), //15 BLOCK_STATE_BIT

    };
    public static final int[] BLOCK_CORNERS_GLASS = new int[]{
            //000 first column
            half_int(2, 0), //00 BLOCK_STATE_FULL
            half_int(2, 14), //01 BLOCK_STATE_LINE_UP
            half_int(2, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(2, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(2, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(2, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(2, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(2, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(2, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(2, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(2, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(2, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(2, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(2, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(2, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(2, 15), //15 BLOCK_STATE_BIT

            //001 second column
            half_int(2, 0), //00 BLOCK_STATE_FULL
            half_int(2, 14), //01 BLOCK_STATE_LINE_UP
            half_int(2, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(2, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(2, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(2, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(2, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(2, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(2, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(2, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(2, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(2, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(2, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(2, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(2, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(2, 15), //15 BLOCK_STATE_BIT

            //010 cracks column 1
            half_int(1, 0), //00 BLOCK_STATE_FULL
            half_int(1, 14), //01 BLOCK_STATE_LINE_UP
            half_int(1, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(1, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(1, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(1, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(1, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(1, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(1, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(1, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(1, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(1, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(1, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(1, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(1, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(1, 15), //15 BLOCK_STATE_BIT

            //011 cracks column 2
            half_int(1, 0), //00 BLOCK_STATE_FULL
            half_int(1, 14), //01 BLOCK_STATE_LINE_UP
            half_int(1, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(1, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(1, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(1, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(1, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(1, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(1, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(1, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(1, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(1, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(1, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(1, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(1, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(1, 15), //15 BLOCK_STATE_BIT

            //100 hammer column
            half_int(6, 0), //00 BLOCK_STATE_FULL
            half_int(6, 14), //01 BLOCK_STATE_LINE_UP
            half_int(6, 12), //02 BLOCK_STATE_LINE_LEFT
            half_int(6, 4), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(6, 13), //04 BLOCK_STATE_LINE_DOWN
            half_int(6, 6), //05 BLOCK_STATE_LINE_HORIZONTAL
            half_int(6, 1), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(6, 8), //07 BLOCK_STATE_LINE_END_LEFT
            half_int(6, 11), //08 BLOCK_STATE_LINE_RIGHT
            half_int(6, 3), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(6, 5), //10 BLOCK_STATE_LINE_VERTICAL
            half_int(6, 10), //11 BLOCK_STATE_LINE_END_UP
            half_int(6, 2), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(6, 7), //13 BLOCK_STATE_LINE_END_RIGHT
            half_int(6, 9), //14 BLOCK_STATE_LINE_END_DOWN
            half_int(6, 15), //15 BLOCK_STATE_BIT
    };
    public static final int[] WALL_CORNERS_DIRT = new int[]{
            half_int(0 + 0, 0), //00 BLOCK_STATE_FULL
            half_int(3 + 16, 2), //01 BLOCK_STATE_LINE_UP
            half_int(2 + 16, 1), //02 BLOCK_STATE_LINE_LEFT
            half_int(2 + 16, 2), //03 BLOCK_STATE_CORNER_UP_LEFT
            half_int(3 + 16, 0), //04 BLOCK_STATE_LINE_DOWN
            half_int(0 + 16, 0), //05 BLOCK_STATE_LINE_HORIZONTAL INVALID
            half_int(2 + 16, 0), //06 BLOCK_STATE_CORNER_DOWN_LEFT
            half_int(0 + 16, 0), //07 BLOCK_STATE_LINE_END_LEFT INVALID
            half_int(4 + 16, 1), //08 BLOCK_STATE_LINE_RIGHT
            half_int(4 + 16, 2), //09 BLOCK_STATE_CORNER_UP_RIGHT
            half_int(0 + 16, 0), //10 BLOCK_STATE_LINE_VERTICAL INVALID
            half_int(0 + 16, 0), //11 BLOCK_STATE_LINE_END_UP INVALID
            half_int(4 + 16, 0), //12 BLOCK_STATE_CORNER_DOWN_RIGHT
            half_int(0 + 16, 0), //13 BLOCK_STATE_LINE_END_RIGHT INVALID
            half_int(0 + 16, 0), //14 BLOCK_STATE_LINE_END_DOWN INVALID
            half_int(0 + 16, 0), //15 BLOCK_STATE_BIT INVALID
    };
}
