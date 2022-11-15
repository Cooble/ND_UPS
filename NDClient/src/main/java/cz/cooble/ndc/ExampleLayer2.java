package cz.cooble.ndc;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.graphics.*;
import cz.cooble.ndc.graphics.font.FontMaterial;
import cz.cooble.ndc.graphics.font.TextBuilder;
import cz.cooble.ndc.graphics.font.TextMesh;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.MousePressEvent;
import cz.cooble.ndc.world.RegistryLoader;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;


public class ExampleLayer2 extends Layer {



    public ExampleLayer2() {
        super("Example");
    }



    private void put(FloatBuffer f, float a,float b){
        f.put(a);
        f.put(b);
    }


    @Override
    public void onAttach() {

        RegistryLoader.load();
        Effect.init();
        ChunkMesh.init();
    }

    @Override
    public void onRender() {
        GCon.enableBlend();
        GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
        ChunkMesh.getProgram().bind();
        ChunkMesh.getAtlas().bind(0);
        ChunkMesh.getCornerAtlas().bind(1);
        ChunkMesh.getVAO().bind();

        FloatBuffer b = BufferUtils.createFloatBuffer(36);

        put(b,0,0);
        put(b,0,0);
        put(b,0,0);

        put(b,1,0);
        put(b,1,0);
        put(b,1,0);

        put(b,1,1);
        put(b,1,1);
        put(b,1,1);

        put(b,0,0);
        put(b,0,0);
        put(b,0,0);

        put(b,1,1);
        put(b,1,1);
        put(b,1,1);

        put(b,0,1);
        put(b,0,1);
        put(b,0,1);

        b.flip();

        ChunkMesh.getVBO().changeData(b,0);

        ChunkMesh.getProgram().setUniformMat4("u_transform", new Matrix4f().identity());

        // chunkProgram.setUniformMat4("u_transform", new Matrix4f(getProjMatrix()).mul(world_matrix));
        // glDrawArrays(GL_TRIANGLES, ChunkMesh.getVertexOffsetToBlockBuffer(mesh.getIndex()), WORLD_CHUNK_AREA * 6);
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }


    @Override
    public void onUpdate() {

    }

    @Override
    public void onEvent(Event e) {

    }
}
