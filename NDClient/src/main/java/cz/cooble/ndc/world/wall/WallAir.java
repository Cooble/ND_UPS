package cz.cooble.ndc.world.wall;

import cz.cooble.ndc.world.BlockStruct;
import cz.cooble.ndc.world.block.BlockTextureAtlas;
import cz.cooble.ndc.world.wall.Wall;

public class WallAir extends Wall {
    public WallAir() {
        super("air",0);
    }

    @Override
    public void onTextureLoaded(BlockTextureAtlas atlas) {}
    @Override
    public int getTextureOffset(int wx, int wy, BlockStruct e) {
        return -1;
    }

    @Override
    public int getCornerOffset(int wx, int wy, BlockStruct b) {
        return 0;
    }
}
