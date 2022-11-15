package cz.cooble.ndc.world;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.Asserter.ND_WARN;
import static cz.cooble.ndc.core.Utils.half_int;
import static cz.cooble.ndc.core.Utils.half_int_object;
import static cz.cooble.ndc.world.Chunk.*;
import static org.joml.Math.*;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;

public class WorldRenderManager {

    //TestQuad* m_test_quad;
    Shader m_sky_program;

    //FrameBufferTexturePair* m_fbo_pair;
    //GreenFilter* m_green_filter;
    // GaussianBlurMultiple* m_blur;
    // ScaleEdgesEffect* m_edge;
    //  AlphaMaskEffect* m_block_mask;

    //background
    FrameBuffer m_bg_fbo;
    FrameBuffer m_bg_layer_fbo;

    //background
    FrameBuffer m_bg_sky_fbo;


    FrameBuffer m_block_fbo;
    FrameBuffer m_wall_fbo;
    FrameBuffer m_sky_fbo;
    FrameBuffer m_entity_fbo;


    //converts from camera space to screen space (-1,-1,1,1)
    Matrix4f m_proj_matrix;

    World m_world;
    Camera m_camera;
    int m_chunk_width = 5, m_chunk_height = 5;
    int last_cx = -10000, last_cy = -10000;//the chunk from which light is computed

    Map<Integer, Integer> m_offset_map = new HashMap<>();//chunkID,offsetInBuffer

    public Matrix4f getProjMatrix() {
        return m_proj_matrix;
    }

    public WorldRenderManager(Camera cam, World world) {
        //test quad
        m_camera = cam;
        m_world = world;

        // m_test_quad = new TestQuad();

        //setup sky
        m_sky_program = Shader.Lib.getOrLoad("res/shaders/Sky.shader");


        //=============prepare FBOs without specifiying dimensions=================

        //setup bg
        var bgInfo = new FrameBuffer.Info(new Texture.Info().
                filterMode(Texture.FilterMode.NEAREST).
                format(Texture.Format.RGBA));
        m_bg_layer_fbo = new FrameBuffer(bgInfo);
        m_bg_fbo = new FrameBuffer(bgInfo);
        m_bg_sky_fbo = new FrameBuffer(bgInfo);

        //setup main light texture

        var lightInfo = new Texture.Info().
                filterMode(Texture.FilterMode.NEAREST).
                format(Texture.Format.RGBA).
                wrapMode(Texture.WrapMode.CLAMP_TO_EDGE);
        var lightInfoSmooth = lightInfo;
        lightInfoSmooth.filterMode(Texture.FilterMode.LINEAR);

        //other
        var defaultInfo = new FrameBuffer.Info(new Texture.Info());
        m_entity_fbo = new FrameBuffer(defaultInfo);
        m_block_fbo = new FrameBuffer(defaultInfo);
        m_wall_fbo = new FrameBuffer(defaultInfo);
        m_sky_fbo = new FrameBuffer(defaultInfo);


        //============OTHER==================
        // m_fbo_pair = new FrameBufferTexturePair();
        onScreenResize();
    }

    public void delete() {
        m_bg_layer_fbo.delete();
        m_bg_fbo.delete();
        m_bg_sky_fbo.delete();
        m_entity_fbo.delete();

        m_block_fbo.delete();
        m_wall_fbo.delete();


        // delete m_block_mask_texture;
        

       /* delete m_green_filter;
        delete m_blur;
        delete m_edge;
        delete m_block_mask;

        delete m_fbo_pair;*/
    }

    ;

    public static final int BLOCK_PIXEL_SIZE = 16;

    public void onScreenResize() {
        //build world view matrix
        float ratioo = (float) App.get().getWindow().getHeight() / (float) App.get().getWindow().getWidth();
        float chunkheightt = 2 * (float) BLOCK_PIXEL_SIZE / (float) App.get().getWindow().getHeight();
        //this 2 means view is from -1 to 1 and not from 0 to 1
        m_proj_matrix = new Matrix4f().scale(ratioo * chunkheightt, chunkheightt, 1);

        //refresh number of chunks
        int screenWidth = App.get().getWindow().getWidth();
        int screenHeight = App.get().getWindow().getHeight();
        float chunkwidth = ((float) screenWidth / (float) BLOCK_PIXEL_SIZE) / (float) WORLD_CHUNK_SIZE;
        float chunkheight = ((float) screenHeight / (float) BLOCK_PIXEL_SIZE) / (float) WORLD_CHUNK_SIZE;
        m_chunk_width = (int) (ceil(chunkwidth) + 3);
        m_chunk_height = (int) (ceil(chunkheight) + 3);


        m_offset_map.clear();

        for (var chunk : ChunkMesh.getChunks())
            chunk.enabled = false;


        Texture.Info info = new Texture.Info();

        //setup green
        info.size(screenWidth, screenHeight);
        info.wrapMode(Texture.WrapMode.CLAMP_TO_EDGE);

        // m_green_filter = new GreenFilter(info);

        // m_fbo_pair.replaceTexture(Texture.create(info));

        info.size(screenWidth, screenHeight);


        //block mask txutre
       /* info = new Texture.Info();
        info.size(screenWidth / 2, screenHeight / 2).filterMode(Texture.FilterMode.NEAREST).format(Texture.Format.RED);

        if (m_block_mask_texture)
            delete m_block_mask_texture;
        m_block_mask_texture = Texture.create(info);*/


       /* info = new Texture.Info();
        info.size(screenWidth, screenHeight);
        if (m_block_mask == nullptr)
        {
            m_block_mask = new AlphaMaskEffect(info);
        }
        else
            m_block_mask.replaceTexture(info);*/


        //light
        //m_light_smooth_fbo.resize(m_chunk_width * WORLD_CHUNK_SIZE * 2, m_chunk_height * WORLD_CHUNK_SIZE * 2);

        //bg
        m_bg_layer_fbo.resize(screenWidth, screenHeight);
        m_bg_sky_fbo.resize(screenWidth, screenHeight);
        m_bg_fbo.resize(screenWidth, screenHeight);

        //other
        m_entity_fbo.resize(screenWidth, screenHeight);
        m_block_fbo.resize(screenWidth, screenHeight);
        m_wall_fbo.resize(screenWidth, screenHeight);
        m_sky_fbo.resize(screenWidth, screenHeight);


       /* if (m_edge == nullptr)
        {
            m_edge = new ScaleEdgesEffect(info);
        }
        else
            m_edge.replaceTexture(info);

        if (m_blur == nullptr)
        {
            //m_blur = new GaussianBlurMultiple(info, {2});
            m_blur = new GaussianBlurMultiple(info, { 2 });
        }
        else
            m_blur.replaceTexture(info);
*/
        last_cx = -1000;


    }

    float minim(float f0, float f1) {
        if (f0 < f1)
            return f0;
        return f1;
    }

    public float narrowFunc(float f) {
        return clamp((f - 0.5f) * 2 + 0.5f, 0.0f, 1.0f);
    }

    void refreshChunkList() {

        Set<Integer> toRemoveList = new HashSet<>();
        Set<Integer> toLoadList = new HashSet<>();

        for (var iterator : m_offset_map.entrySet())
            toRemoveList.add(iterator.getKey()); //get all loaded chunks

        for (int x = 0; x < m_chunk_width; x++)
            for (int y = 0; y < m_chunk_height; y++) {
                var targetX = last_cx + x;
                var targetY = last_cy + y;
                if (
                        targetX < 0
                                || targetY < 0
                                || targetX >= (m_world.getInfo().chunk_width)
                                || targetY >= (m_world.getInfo().chunk_height))
                    continue;
                int mid = half_int(targetX, targetY);
                var cc = m_world.getChunk(targetX, targetY);
                if (cc == null) {
                    //  ND_WARN("Cannot render unloaded chunk {},{}", targetX, targetY);
                    continue;
                }
                var eeee = m_offset_map.get(mid);
                if (eeee == null)
                    toLoadList.add(mid);

                else if (cc.isDirty()) {
                    ChunkMesh.getChunks().get(m_offset_map.get(mid)).updateMesh(m_world, cc);
                    cc.markDirty(false);
                }
                toRemoveList.remove(mid);
            }
        for (int removed : toRemoveList) {
            ChunkMesh.getChunks().get(m_offset_map.get(removed)).enabled = false;
            m_offset_map.remove(removed);
        }
        int lastFreeChunk = 0;
        for (int loade : toLoadList) {
            var loaded = half_int_object(loade);
            boolean foundFreeChunk = false;
            var c = ChunkMesh.getFreeChunk();
            c.getPos().x = loaded.x * WORLD_CHUNK_SIZE;
            c.getPos().y = loaded.y * WORLD_CHUNK_SIZE;

            var chunk = m_world.getChunk(loade);
            chunk.markDirty(false);
            c.updateMesh(m_world, chunk);

            m_offset_map.put(loade, c.getIndex());
            foundFreeChunk = true;

            ASSERT(foundFreeChunk, "This shouldnt happen"); //chunks meshes should have static number
        }
    }

    public void update() {
        int cx = (int) (m_camera.getPosition().x / WORLD_CHUNK_SIZE - m_chunk_width / 2.0f);
        int cy = (int) (m_camera.getPosition().y / WORLD_CHUNK_SIZE - m_chunk_height / 2.0f);

        if (cx != last_cx || cy != last_cy || m_world.hasChunkChanged()) {
            last_cx = cx;
            last_cy = cy;
            refreshChunkList();
        }

        for (var iterator : m_offset_map.entrySet()) {
            int id = iterator.getKey();
            var c = m_world.getChunk(id);
            if (c != null) {
                if (c.isDirty()) {
                    ChunkMesh.getChunks().get(iterator.getValue()).updateMesh(m_world, c);
                    c.markDirty(false);
                }
            } else
                ND_WARN("Worldrendermanager cannot find chunk");
        }

    }

    int getChunkIndex(int cx, int cy) {
        int id = half_int(cx, cy);
        var out = m_offset_map.get(id);
        if (out == null)
            return -1;
        return out;
    }

    Vector4f getSkyColor(float y) {

        var upColor = new Vector4f(0, 0, 0, 1);
        var downColor = new Vector4f(0.1f, 0.8f, 1, 1);

        return downColor;
    }

    private void put(FloatBuffer f, float a, float b) {
        f.put(a);
        f.put(b);
    }


    public void render(BatchRenderer2D batchRenderer, FrameBuffer fbo) {

        //bg
        //renderBiomeBackgroundToFBO(batchRenderer);

        //chunk render
        var chunkProgram = ChunkMesh.getProgram();

        //walls
        m_wall_fbo.bind();
        {
            m_wall_fbo.clear(GCon.BufferBit.COLOR);

            //background
           /* GCon.enableBlend();
            GCon.blendEquation(GCon.BlendEquation.FUNC_ADD);
            GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
            Effect.render(m_bg_fbo.getAttachment(), m_wall_fbo);

            m_wall_fbo.bind();*/
            GCon.enableBlend();

            //walls
            chunkProgram.bind();
            ChunkMesh.getAtlas().bind(0);
            ChunkMesh.getCornerAtlas().bind(1);
            ChunkMesh.getVAO().bind();

            for (var mesh : ChunkMesh.getChunks()) {
                if (!(mesh.enabled))
                    continue;

                var worldMatrix = new Matrix4f().translate(
                        mesh.getPos().x - m_camera.getPosition().x,
                        mesh.getPos().y - m_camera.getPosition().y, 0.0f);

                worldMatrix=worldMatrix.scaleXY(0.5f,0.5f);
                chunkProgram.setUniformMat4("u_transform", new Matrix4f(getProjMatrix()).mul(worldMatrix));
                glDrawArrays(GL_TRIANGLES, ChunkMesh.getVertexOffsetToWallBuffer(mesh.getIndex()), WORLD_CHUNK_AREA * 4 * 6);
            }
        }
        m_wall_fbo.unbind();


        //blocks
        m_block_fbo.bind();
        {
            m_block_fbo.clear(GCon.BufferBit.COLOR);

            GCon.enableBlend();
            GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
            chunkProgram.bind();
            ChunkMesh.getAtlas().bind(0);
            ChunkMesh.getCornerAtlas().bind(1);
            ChunkMesh.getVAO().bind();
            for (var mesh : ChunkMesh.getChunks()) {
                if (!mesh.enabled)
                    continue;

                var world_matrix = new Matrix4f().translate(
                        mesh.getPos().x - m_camera.getPosition().x,
                        mesh.getPos().y - m_camera.getPosition().y, 0.0f);

                chunkProgram.setUniformMat4("u_transform", new Matrix4f(getProjMatrix()).mul(world_matrix));
                glDrawArrays(GL_TRIANGLES, ChunkMesh.getVertexOffsetToBlockBuffer(mesh.getIndex()), WORLD_CHUNK_AREA * 6);
            }
        }

        m_block_fbo.unbind();

        fbo.bind();
        fbo.clear(GCon.BufferBit.COLOR | GCon.BufferBit.DEPTH);
        //sky render
        GCon.disableBlend();

        float CURSOR_Y = (float) App.get().getWindow().getHeight() / BLOCK_PIXEL_SIZE + m_camera.getPosition().y;
        float CURSOR_YY = -(float) App.get().getWindow().getHeight() / BLOCK_PIXEL_SIZE + m_camera.getPosition().y;

        m_sky_program.bind();
        m_sky_program.setUniformVec4f("u_up_color", getSkyColor(CURSOR_Y));
        m_sky_program.setUniformVec4f("u_down_color", getSkyColor(CURSOR_YY));
        Effect.renderDefaultVAO();

        GCon.enableBlend();
        GCon.blendEquation(GCon.BlendEquation.FUNC_ADD);
        GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
        Effect.render(m_bg_sky_fbo.getAttachment(), fbo);
        GCon.enableBlend();
        Effect.render(m_wall_fbo.getAttachment(), fbo);
        GCon.enableBlend();
        Effect.render(m_block_fbo.getAttachment(), fbo);
        GCon.enableBlend();
        fbo.bind();
    }

    public FrameBuffer getEntityFBO() {
        return m_entity_fbo;
    }
}
