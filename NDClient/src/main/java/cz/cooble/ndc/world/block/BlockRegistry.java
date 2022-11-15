package cz.cooble.ndc.world.block;

import cz.cooble.ndc.world.wall.Wall;

import java.util.ArrayList;
import java.util.List;

public class BlockRegistry {
    public static List<Block> m_blocks = new ArrayList<>();
    public static List<Wall> m_walls = new ArrayList<>();

    public static List<Block> getBlocks() {return m_blocks;}

    public static List<Wall> getWalls() {return m_walls;}

    public static void initTextures(BlockTextureAtlas atlas) {
        for (Block block : m_blocks)
            block.onTextureLoaded(atlas);
        for (Wall wall : m_walls)
            wall.onTextureLoaded(atlas);
    }

    public static void registerBlock(Block block) {
        while (m_blocks.size() <= block.getID())
            m_blocks.add(null);
        m_blocks.set(block.getID(), block);
    }

    public static void registerWall(Wall wall) {
        while (m_walls.size() <= wall.getID())
            m_walls.add(null);
        m_walls.set(wall.getID(), wall);
    }

    public static Block getBlock(int block_id) {
        return m_blocks.get(block_id);
    }

    public static Wall getWall(int wall_id) {
        return m_walls.get(wall_id);

    }
}
