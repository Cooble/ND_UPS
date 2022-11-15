package cz.cooble.ndc.world.block;

public class BlockSnowBricks extends Block{

    public BlockSnowBricks() {
        super(BlockID.BLOCK_SNOW_BRICKS,"snow_bricks");
        setFlag(BLOCK_FLAG_HAS_BIG_TEXTURE);
        m_block_connect_group = BlockID.CONNECT_GROUP_DIRT;
        m_corner_translate_array = BlockDatas.BLOCK_CORNERS_GLASS;
    }
}
