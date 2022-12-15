package cz.cooble.ndc.graphics;

import cz.cooble.ndc.world.BlockStruct;
import cz.cooble.ndc.world.Chunk;
import cz.cooble.ndc.world.World;
import cz.cooble.ndc.world.block.Block;
import cz.cooble.ndc.world.block.BlockRegistry;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static cz.cooble.ndc.core.Utils.half_int_object;
import static cz.cooble.ndc.graphics.VertexBuffer.Usage.DYNAMIC_DRAW;
import static cz.cooble.ndc.world.Chunk.WORLD_CHUNK_AREA;
import static cz.cooble.ndc.world.Chunk.WORLD_CHUNK_SIZE;

public class ChunkMesh {

    public static final int BLOCK_TEXTURE_ATLAS_SIZE = 32;//icons in row
    public static final int BLOCK_CORNER_ATLAS_SIZE = 16;//icons in row

    public static final int WALL_TEXTURE_ATLAS_SIZE = BLOCK_TEXTURE_ATLAS_SIZE * 2;//icons in row
    public static final int WALL_CORNER_ATLAS_SIZE = BLOCK_CORNER_ATLAS_SIZE * 2;//icons in row

    public static final int BLOCK_ATLAS_PIXEL_WIDTH = BLOCK_TEXTURE_ATLAS_SIZE * 8;//pixel width of texture (block is considered 8 pixels big)

    //1 over factor = ratio of bordercolor section to the whole block texture atlas
    public static final int EDGE_COLOR_TRANSFORMATION_FACTOR = 4;//how much should i divide texture pos to get to the border color

    public static final int POS_VERTEX_DATA_FLOAT_COUNT = 6;

    public static final int BUFF_LIGHT_SIZE = WORLD_CHUNK_AREA;
    public static final int BUFF_BLOCK_SIZE = WORLD_CHUNK_AREA * 6 * POS_VERTEX_DATA_FLOAT_COUNT;
    public static final int BUFF_WALL_SIZE = WORLD_CHUNK_AREA * 4 * 6 * POS_VERTEX_DATA_FLOAT_COUNT;

    static VertexBuffer.Layout s_layout;

    static Shader s_program;

    static Texture s_texture;
    static Texture s_texture_corners;

    static boolean alreadyLoaded = false;
    public static void init(){
        if(alreadyLoaded)
            return;
        alreadyLoaded=true;

        Texture.Info info = new Texture.Info();

        s_texture = new Texture(info.path("res/images/blockAtlas/atlas.png").filterMode(Texture.FilterMode.NEAREST));
        s_texture_corners = new Texture(info.path("res/images/atlas/corners.png"));

        s_program = Shader.Lib.getOrLoad("res/shaders/ChunkNew.shader");
        s_program.bind();
        s_program.setUniform1i("u_texture", 0);
        s_program.setUniform1i("u_corners", 1);
        s_program.setUniform1i("u_texture_atlas_pixel_width_corner", EDGE_COLOR_TRANSFORMATION_FACTOR);//scale factor of determining color of corner border (4 means divide pixel pos by 4 to get to the border color)

        //todo when changing blockpixels size this wont work you need to specify pixel size of texture
        s_program.setUniform1i("u_texture_atlas_pixel_width", BLOCK_ATLAS_PIXEL_WIDTH);//for every block we have 8 pixels in texture
        s_program.unbind();

        s_layout = new VertexBuffer.Layout(
                g_typ.VEC2,//pos
                g_typ.VEC2,//uv0
                g_typ.VEC2//uv1
        );

        resize(10);
    }

    public static VertexBuffer.Layout getLayout() {return s_layout;}

    public static Shader getProgram() {return s_program;}

    public static Texture getAtlas() {return s_texture;}

    public static Texture getCornerAtlas() {return s_texture_corners;}

    private static List<ChunkMeshInstance> s_instances = new ArrayList<>();

    private static VertexArray s_vao;
    private static VertexBuffer s_vbo;
    private static int s_chunk_count;


    private static void resize(int chunkCount) {
        if (s_chunk_count < chunkCount) {
            s_chunk_count = chunkCount;

            if (s_vbo != null) {
                s_vbo.delete();
                s_vao.delete();
            }
            while (s_instances.size() < chunkCount)
                s_instances.add(new ChunkMeshInstance(s_instances.size()));

            s_vbo = new VertexBuffer((chunkCount * (BUFF_BLOCK_SIZE + BUFF_WALL_SIZE)) * 4, DYNAMIC_DRAW);
            s_vbo.setLayout(ChunkMesh.getLayout());

            s_vao = new VertexArray();
            s_vao.addBuffer(s_vbo);
        }
    }

    public static void updateVBO(int index) {
        var inst = s_instances.get(index);

        s_vbo.changeData(inst.m_blocks, BUFF_BLOCK_SIZE * index * 4);
        s_vbo.changeData(inst.m_walls, BUFF_BLOCK_SIZE * s_chunk_count * 4 + BUFF_WALL_SIZE * index * 4 );
    }

    public static VertexArray getVAO() {
        return s_vao;
    }

    public static int getVertexOffsetToBlockBuffer(int index) {return WORLD_CHUNK_AREA * 6 * index;}

    public static int getVertexOffsetToWallBuffer(int index) {return  WORLD_CHUNK_AREA * 6 * s_chunk_count + 4 * WORLD_CHUNK_AREA * 6 * index;}

    public static ChunkMeshInstance getFreeChunk() {
        for (var instance : s_instances)
            if (!instance.enabled) {
                instance.enabled = true;
                return instance;
            }
        resize(s_instances.size() + 2);
        return getFreeChunk();
    }

    public static List<ChunkMeshInstance> getChunks() {
        return s_instances;
    }

    public static VertexBuffer getVBO() {
        return s_vbo;
    }

    public static class ChunkMeshInstance {
        float[] m_blocks = new float[BUFF_BLOCK_SIZE];
        float[] m_walls = new float[BUFF_WALL_SIZE];

        Vector2f m_pos=new Vector2f();
        final int m_mesh_index;
        public boolean enabled;

        ChunkMeshInstance(int meshIdx) {
            this.m_mesh_index = meshIdx;
        }

        public Vector2f getPos() {
            return m_pos;
        }

        private int m_index;

        private void buffMeBlock(float x, float y) {
            m_blocks[m_index++] = x;
            m_blocks[m_index++] = y;
        }
        private void buffMeWall(float x, float y) {
            m_walls[m_index++] = x;
            m_walls[m_index++] = y;
        }

        public void updateMesh(World world, Chunk chunk) {

            float co = 1.0f / BLOCK_TEXTURE_ATLAS_SIZE;
            float co_corner = 1.0f / BLOCK_CORNER_ATLAS_SIZE;

            for (int y = 0; y < WORLD_CHUNK_SIZE; y++) {
                for (int x = 0; x < WORLD_CHUNK_SIZE; x++) {
                    BlockStruct bs = chunk.block(x, y);
                    Block blok = BlockRegistry.getBlock(bs.block_id);
                    var t_offset_i = blok.getTextureOffset(x, y, bs);
                    var t_offset = half_int_object(t_offset_i);

                    var t_corner_offset = half_int_object(blok.getCornerOffset(x, y, bs));

                    m_index = (POS_VERTEX_DATA_FLOAT_COUNT * (y * WORLD_CHUNK_SIZE + x)) * 6;

                    int oldX = x;
                    if (t_offset_i == -1)
                        x = -1000;//discard quad

                    //0,0
                    {
                        buffMeBlock(x, y);
                        buffMeBlock(t_offset.x * co, t_offset.y * co);
                        buffMeBlock(t_corner_offset.x * co_corner, t_corner_offset.y * co_corner);
                    }
                    //1,0
                    {
                        buffMeBlock(x + 1, y);
                        buffMeBlock((t_offset.x + 1) * co, t_offset.y * co);
                        buffMeBlock((t_corner_offset.x + 1) * co_corner, t_corner_offset.y * co_corner);
                    }
                    //1,1
                    {
                        buffMeBlock(x + 1, y + 1);
                        buffMeBlock((t_offset.x + 1) * co, (t_offset.y + 1) * co);
                        buffMeBlock((t_corner_offset.x + 1) * co_corner, (t_corner_offset.y + 1) * co_corner);

                    }
                    //0,0
                    {
                        buffMeBlock(x, y);
                        buffMeBlock(t_offset.x * co, t_offset.y * co);
                        buffMeBlock(t_corner_offset.x * co_corner, t_corner_offset.y * co_corner);
                    }
                    //1,1
                    {
                        buffMeBlock(x + 1, y + 1);
                        buffMeBlock((t_offset.x + 1) * co, (t_offset.y + 1) * co);
                        buffMeBlock((t_corner_offset.x + 1) * co_corner, (t_corner_offset.y + 1) * co_corner);
                    }
                    //0,1
                    {
                        buffMeBlock(x, y + 1);
                        buffMeBlock(t_offset.x * co, (t_offset.y + 1) * co);
                        buffMeBlock(t_corner_offset.x * co_corner, (t_corner_offset.y + 1) * co_corner);
                    }
                    x = oldX;
                }
            }

            co = 1.0f / WALL_TEXTURE_ATLAS_SIZE;
            co_corner = 1.0f / WALL_CORNER_ATLAS_SIZE;

            for (int y = 0; y < WORLD_CHUNK_SIZE * 2; y++) {
                for (int x = 0; x < WORLD_CHUNK_SIZE * 2; x++) {
                    var bs = chunk.block(x / 2, y / 2);
                    var wall = BlockRegistry.getWall(bs.wall_id[(y & 1) * 2 + (x & 1)]);

                    var t_offset_i = wall.getTextureOffset(x, y, bs);
                    var t_offset = half_int_object(t_offset_i);

                    var t_corner_offset = half_int_object(wall.getCornerOffset(x, y, bs));

                    m_index = POS_VERTEX_DATA_FLOAT_COUNT * (y * WORLD_CHUNK_SIZE * 2 + x) * 6;

                    int oldX = x;
                    if (t_offset_i == -1) {
                        x = -1000;//discard quad
                    }
                    //0,0
                    {
                        buffMeWall(x, y);
                        buffMeWall(t_offset.x * co, t_offset.y * co);
                        buffMeWall(t_corner_offset.x * co_corner, t_corner_offset.y * co_corner);
                    }
                    //1,0
                    {
                        buffMeWall(x + 1, y);
                        buffMeWall((t_offset.x + 1) * co, t_offset.y * co);
                        buffMeWall((t_corner_offset.x + 1) * co_corner, t_corner_offset.y * co_corner);
                    }
                    //1,1
                    {
                        buffMeWall(x + 1, y + 1);
                        buffMeWall((t_offset.x + 1) * co, (t_offset.y + 1) * co);
                        buffMeWall((t_corner_offset.x + 1) * co_corner, (t_corner_offset.y + 1) * co_corner);

                    }
                    //0,0
                    {
                        buffMeWall(x, y);
                        buffMeWall(t_offset.x * co, t_offset.y * co);
                        buffMeWall(t_corner_offset.x * co_corner, t_corner_offset.y * co_corner);
                    }
                    //1,1
                    {
                        buffMeWall(x + 1, y + 1);
                        buffMeWall((t_offset.x + 1) * co, (t_offset.y + 1) * co);
                        buffMeWall((t_corner_offset.x + 1) * co_corner, (t_corner_offset.y + 1) * co_corner);
                    }
                    //0,1
                    {
                        buffMeWall(x, y + 1);
                        buffMeWall(t_offset.x * co, (t_offset.y + 1) * co);
                        buffMeWall(t_corner_offset.x * co_corner, (t_corner_offset.y + 1) * co_corner);
                    }
                    x = oldX;
                }
            }

            ChunkMesh.updateVBO(m_mesh_index);
        }

        public int getIndex() {
            return m_mesh_index;
        }
    }
}
