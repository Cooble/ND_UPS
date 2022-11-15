package cz.cooble.ndc.world.block;

import cz.cooble.ndc.world.BlockStruct;
import cz.cooble.ndc.world.World;

public class BlockAir extends Block{
    public BlockAir() {
        super(0,"air");
        m_collision_box_size=0;
    }

    @Override
    void onTextureLoaded(BlockTextureAtlas atlas) {
    }

    @Override
    public int getTextureOffset(int x, int y, BlockStruct b) {
        return -1;
    }

    @Override
    public boolean onNeighborBlockChange(World world, int x, int y) {
        return false;
    }
}
