package cz.cooble.ndc.world;

import cz.cooble.ndc.core.NBT;
import cz.cooble.ndc.world.block.Block;
import cz.cooble.ndc.world.block.BlockRegistry;
import org.joml.Vector4f;
import org.lwjgl.opengl.NVRobustnessVideoMemoryPurge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static cz.cooble.ndc.Asserter.ND_WARN;
import static cz.cooble.ndc.core.Utils.BIT;
import static cz.cooble.ndc.core.Utils.half_int;
import static cz.cooble.ndc.world.Chunk.*;
import static org.joml.Math.clamp;

public class World {

    public Block getBlockInstance(int x, int y) {
        var str = getBlock(x, y);
        if (str == null)
            return BlockRegistry.getBlock(0);
        return BlockRegistry.getBlock(str.block_id);
    }

    public WorldInfo getInfo() {
        return m_info;
    }

    private boolean m_has_chunk_changed;
    public boolean hasChunkChanged() {
        var out = m_has_chunk_changed;
        m_has_chunk_changed = false;
        return out;
    }

    public HashMap<Integer,Chunk> getChunks() {
        return m_chunks;
    }

    public List<Integer> getLoadedEntities() {
        //todo add loaded entities
        return Collections.emptyList();
    }

    public boolean isAir(int x, int y) {
        var b = getBlock(x, y);
        return b == null || b.isAir();
    }

    public static class WorldInfo {
        public int chunk_width=10, chunk_height=10;
        public String name="worldName";
    }

    private HashMap<Integer, Chunk> m_chunks = new HashMap<>();

    private NBT m_world_nbt = new NBT();

    private WorldInfo m_info = new WorldInfo();

    public void onUpdate() {
        tick();
    }

    public void tick() {
      /*  nd::temp_vector<EntityID> entityArrayBuff(m_entity_array.size());
        memcpy(entityArrayBuff.data(), m_entity_array.data(), m_entity_array.size() * sizeof(EntityID));

        for (var id : entityArrayBuff)
        {
            WorldEntity* entity = m_entity_manager.entity(id);
            if (entity)
            {
                entity.update(*this);
                if (entity.isMarkedDead())
                    killEntity(entity.getID());
            }
        }

        m_tile_entity_array_buff = std::unordered_map<int64_t, EntityID>(m_tile_entity_map); //lets just copy

        for (var& pair : m_tile_entity_array_buff)
        {
            WorldEntity* entity = m_entity_manager.entity(pair.second);
            if (entity)
            {
                entity.update(*this);
                if (entity.isMarkedDead())
                    killEntity(entity.getID());
            }
        }
        m_particle_manager.update();*/
    }

//====================CHUNKS=========================

    public Chunk getChunk(int cx, int cy) {
        return m_chunks.get(half_int(cx, cy));
    }

    public Chunk getChunk(int id) {
        return m_chunks.get(id);
    }

    public void loadChunk(int cx, int cy) {
        Chunk c = new Chunk(cx,cy);
        m_chunks.put(half_int(cx,cy),c);
        WorldGen.gen(this,c);
    }
    public void setChunk(Chunk c){
        m_chunks.put(c.chunkID(),c);
    }

    public final int maskUp = BIT(0);
    public final int maskDown = BIT(1);
    public final int maskLeft = BIT(2);
    public final int maskRight = BIT(3);
    public final int maskGen = BIT(4);
    public final int maskFreshlyOnlyLoaded = BIT(5); //chunk was only loaded by thread, no gen neccessary
    public final int maskAllSides = maskUp | maskDown | maskLeft | maskRight;

//=========================BLOCKS==========================

    public BlockStruct getBlock(int x, int y) {
        var ch = m_chunks.get(Chunk.getChunkIDFromWorldPos(x, y));
        if (ch == null)
            return null;
        return ch.block(x & (WORLD_CHUNK_SIZE - 1), y & (WORLD_CHUNK_SIZE - 1));
    }

    public BlockStruct getBlock(float x, float y) {
       return getBlock((int)x,(int)y);
    }

    private void setInnerBlock(int x, int y, BlockStruct b) {
        var ch = m_chunks.get(Chunk.getChunkIDFromWorldPos(x, y));
        if (ch == null)
            return;

        ch.setBlock(x & (WORLD_CHUNK_SIZE - 1), y & (WORLD_CHUNK_SIZE - 1), b);
    }

    static BlockStruct air = new BlockStruct();

    public BlockStruct getBlockOrAir(int x, int y) {
        var ch = m_chunks.get(Chunk.getChunkIDFromWorldPos(x, y));
        if (ch == null)
            return air;
        return ch.block(x & (WORLD_CHUNK_SIZE - 1), y & (WORLD_CHUNK_SIZE - 1));
    }

    public void setBlockWithNotify(int x, int y, BlockStruct newBlock) {
        var blok = getBlock(x, y);
        if (blok == null)
            return;

        var regOld = BlockRegistry.getBlock(blok.block_id);
        var regNew = BlockRegistry.getBlock(newBlock.block_id);

        System.arraycopy(blok.wall_id, 0, newBlock.wall_id, 0, newBlock.wall_id.length);
        System.arraycopy(blok.wall_corner, 0, newBlock.wall_corner, 0, newBlock.wall_corner.length);

        regOld.onBlockDestroyed(this, null, x, y, blok);

        setInnerBlock(x, y, newBlock);

        regNew.onBlockPlaced(this, null, x, y, newBlock);
        regNew.onNeighborBlockChange(this, x, y);
        onBlocksChange(x, y, 0);

        getChunk(Chunk.getChunkIDFromWorldPos(x,y)).markDirty(true);
    }

    public void setBlock(int x, int y, BlockStruct newBlock) {
        var blok = getBlock(x, y);
        if (blok == null)
            return;

        System.arraycopy(blok.wall_id, 0, newBlock.wall_id, 0, newBlock.wall_id.length);
        System.arraycopy(blok.wall_corner, 0, newBlock.wall_corner, 0, newBlock.wall_corner.length);

        setInnerBlock(x, y, newBlock);
    }

    public final int MAX_BLOCK_UPDATE_DEPTH = 20;

    public void onBlocksChange(int x, int y, int deep) {
        ++deep;
        if (deep >= MAX_BLOCK_UPDATE_DEPTH) {
            ND_WARN("Block update too deep! protecting stack");
            return;
        }

        var p = getBlock(x, y + 1);
        if (p != null && BlockRegistry.getBlock(p.block_id).onNeighborBlockChange(this, x, y + 1)) {
            getChunk(Chunk.getChunkIDFromWorldPos(x, y + 1)).markDirty(true);
            onBlocksChange(x, y + 1, deep);
        }
        p = getBlock(x, y - 1);
        if (p != null && BlockRegistry.getBlock(p.block_id).onNeighborBlockChange(this, x, y - 1)) {
            getChunk(Chunk.getChunkIDFromWorldPos(x, y - 1)).markDirty(true);
            onBlocksChange(x, y - 1, deep);
        }
        p = getBlock(x + 1, y);
        if (p != null && BlockRegistry.getBlock(p.block_id).onNeighborBlockChange(this, x + 1, y)) {
            getChunk(Chunk.getChunkIDFromWorldPos(x + 1, y)).markDirty(true);
            onBlocksChange(x + 1, y, deep);
        }
        p = getBlock(x - 1, y);
        if (p != null && BlockRegistry.getBlock(p.block_id).onNeighborBlockChange(this, x - 1, y)) {
            getChunk(Chunk.getChunkIDFromWorldPos(x - 1, y)).markDirty(true);
            onBlocksChange(x - 1, y, deep);
        }
    }

//=========================WALLS===========================

    public void setWallWithNotify(int x, int y, int wall_id) {
        var blok = getBlock(x, y);
        if (blok == null)
            return;

        if (!blok.isWallFullyOccupied() && wall_id == 0) //cannot erase other blocks parts on this shared block
            return;
        blok.setWall(wall_id);
        onWallsChange(x, y,blok);

        getChunk(getChunkIDFromWorldPos(x,y)).markDirty(true);
    }

    public boolean isBlockValid(int x, int y) {
        return x >= 0 && y >= 0 && x < m_info.chunk_width * WORLD_CHUNK_SIZE && y < m_info.chunk_height * WORLD_CHUNK_SIZE;
    }


    public void onWallsChange(int xx, int yy, BlockStruct blok) {
        BlockRegistry.getWall(blok.wallID()).onNeighbourWallChange(this, xx, yy);

        for (int x = -2; x < 3; ++x) {
            for (int y = -2; y < 3; ++y) {
                if (x == 0 && y == 0)
                    continue;
                if (isBlockValid(x + xx, y + yy)) {
                    var b = getBlock(x + xx, y + yy);
                    if (b.isWallFullyOccupied()) {
                        //i cannot call this method on some foreign wall pieces
                        BlockRegistry.getWall(b.wallID()).onNeighbourWallChange(this, x + xx, y + yy);
                        var c = getChunk(getChunkIDFromWorldPos(x + xx, y + yy));
                        c.markDirty(true); //mesh needs to be updated
                    }
                }
            }
        }
    }

//===================ENTITIES============================


//======================WORLD IO=========================

}
