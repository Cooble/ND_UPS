package cz.cooble.ndc.world;

import cz.cooble.ndc.world.block.BlockDatas;
import cz.cooble.ndc.world.block.BlockID;
import cz.cooble.ndc.world.block.BlockRegistry;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.world.Chunk.WORLD_CHUNK_SIZE;
import static cz.cooble.ndc.world.Chunk.getChunkIDFromWorldPos;

public class WorldGen {

    public static void gen(World w, Chunk c) {
        final int horizont = 24;

        for (int y = 0; y < WORLD_CHUNK_SIZE; y++) {
            for (int x = 0; x < WORLD_CHUNK_SIZE; x++) {
                var b = c.block(x, y);
                if (y > horizont || (x&3)==0)
                    b.block_id = 0;
                else if (y == horizont) {
                    b.block_id = BlockID.BLOCK_DIRT;
                } else if (y > horizont - 5) {
                    b.block_id = BlockID.BLOCK_SNOW;
                } else if (y == horizont - 6)
                    b.block_id = BlockID.BLOCK_SNOW_BRICKS;
                else
                    b.block_id = 0;
            }

        }

        var offsetX = c.getCX() * WORLD_CHUNK_SIZE;
        var offsetY = c.getCY() * WORLD_CHUNK_SIZE;

        for (int y = 1; y < WORLD_CHUNK_SIZE - 1; y++) {
            for (int x = 1; x < WORLD_CHUNK_SIZE - 1; x++)
            //boundaries will be updated after all other adjacent chunks are generated
            {
                var block = c.block(x, y);

                var worldx = offsetX + x;
                var worldy = offsetY + y;
                if (!block.isAir())
                    BlockRegistry.getBlock(block.block_id).onNeighborBlockChange(w, worldx, worldy);
                if (!block.isWallFree())
                    BlockRegistry.getWall(block.wallID()).onNeighbourWallChange(w, worldx, worldy);
            }
        }
    }
}
