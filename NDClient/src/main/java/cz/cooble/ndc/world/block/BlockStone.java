package cz.cooble.ndc.world.block;

public class BlockStone extends Block{
    public BlockStone() {
        super(BlockID.BLOCK_STONE, "stone");
        setFlag(BLOCK_FLAG_HAS_BIG_TEXTURE);
        m_block_connect_group = BlockID.CONNECT_GROUP_DIRT;

    }
}
