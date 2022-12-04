package cz.cooble.ndc.world;

import java.io.*;
import java.nio.ByteBuffer;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.core.Utils.BIT;
import static cz.cooble.ndc.core.Utils.half_int;

public class Chunk {

    public static final int WORLD_CHUNK_BIT_SIZE = 5;
    public static final int WORLD_CHUNK_SIZE = 32;
    public static final int WORLD_CHUNK_AREA = WORLD_CHUNK_SIZE * WORLD_CHUNK_SIZE;

    public static final int BITS_FOR_CHUNK_LOC = 16;

    public static final int CHUNK_DIRTY_FLAG = BIT(1); //in next update chunk graphics will be reloaded into chunkrenderer
    public static final int CHUNK_LOCKED_FLAG = BIT(2); //dont unload this chunk its being worked on by main thread


    private int m_x, m_y;
    private int m_flags = CHUNK_DIRTY_FLAG;

    BlockStruct[] m_blocks = new BlockStruct[WORLD_CHUNK_AREA];

    public Chunk(int x, int y) {
        for (int i = 0; i < m_blocks.length; i++)
            m_blocks[i] = new BlockStruct();
        this.m_x = x;
        this.m_y = y;
    }

    @Override
    public String toString() {
        return "Chunk["+m_x+", "+m_y+"]";
    }

    public long last_save_time;

    private boolean isGenerated;

    public int getCX() {return m_x;}

    public int getCY() {return m_y;}

    public int chunkID() {return half_int(m_x, m_y);}

    // cannot unload locked chunk
    public boolean isLocked() {
        return (m_flags & CHUNK_LOCKED_FLAG) != 0 /*|| (!m_light_job.isDone())*/;
    }

    public boolean isDirty() {return (m_flags & CHUNK_DIRTY_FLAG) != 0;}


    public boolean isGenerated() {return isGenerated;}

    public void setGenerated(boolean generated) {
        isGenerated = generated;
    }

    public void lock(boolean lock) {
        if (lock) m_flags |= CHUNK_LOCKED_FLAG;
        else m_flags &= ~CHUNK_LOCKED_FLAG;
    }

    public BlockStruct block(int x, int y) {return m_blocks[y << WORLD_CHUNK_BIT_SIZE | x];}

    public void setBlock(int x, int y, BlockStruct b) {m_blocks[y << WORLD_CHUNK_BIT_SIZE | x] = b;}


    public void markDirty(boolean dirty) {
        if (dirty) m_flags |= CHUNK_DIRTY_FLAG;
        else m_flags &= ~CHUNK_DIRTY_FLAG;
    }

    public void markDirty() {
        markDirty(true);
    }


    public static int getChunkIDFromWorldPos(int wx, int wy) {
        return half_int(wx >> WORLD_CHUNK_BIT_SIZE, wy >> WORLD_CHUNK_BIT_SIZE);
    }
    static final int ChunkPieceSize = 1040;//52 blocks
    static final int ChunkPieceBlockCount = 52;


    public void deserialize(ByteBuffer d,int piece) {
        for (int i = ChunkPieceBlockCount*piece; i < ChunkPieceBlockCount*(piece+1) && i<m_blocks.length; i++) {
            var b = m_blocks[i];
           // must be little endian
            b.block_metadata = d.getInt();
            b.wall_id[0] = d.getShort();
            b.wall_id[1] = d.getShort();
            b.wall_id[2] = d.getShort();
            b.wall_id[3] = d.getShort();
            d.get(b.wall_corner);
            b.block_id = d.getShort();
            b.block_corner = d.get();
            d.get();//last padding byte
        }
        int i = 0;
    }

    public void serialize(ByteBuffer d) {
        /*for (BlockStruct b : m_blocks) {
            d.putShort((short) b.block_id);
            d.putInt(b.block_metadata);
            d.put((byte) b.block_corner);
            d.putShort((short) b.wall_id[0]);
            d.putShort((short) b.wall_id[1]);
            d.putShort((short) b.wall_id[2]);
            d.putShort((short) b.wall_id[3]);
            d.put(b.wall_corner);
        }*/
    }
}
