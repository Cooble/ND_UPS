package cz.cooble.ndc.graphics;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

import static cz.cooble.ndc.graphics.GUtils.wrapBuffer;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glDrawArrays;

public class Effect {
    private static VertexBuffer s_vbo;
    private static VertexArray s_vao;
    private static Shader s_shader;
    public static void init(){
        float[] f = new float[]
                {
                        //this is clockwise
                        /*1, -1, 1, 0,
                        -1, -1, 0, 0,
                        1, 1, 1, 1,
                        -1, 1, 0, 1,*/

                        //this is counterclockwise
                        //   x,y   u,v
                        -1, 1, 0, 1,
                        -1, -1, 0, 0,
                        1, 1, 1, 1,
                        1, -1, 1, 0,

                };

        s_vbo = new VertexBuffer(wrapBuffer(f), VertexBuffer.Usage.STATIC_DRAW);
        var layout = new VertexBuffer.Layout(
                g_typ.VEC2, //pos
                g_typ.VEC2 //uv
        );

        s_vbo.setLayout(layout);
        s_vao = new VertexArray();

        s_vao.addBuffer(s_vbo);

        s_shader = Shader.Lib.getOrLoad("res/shaders/TextureQuad.shader");
        s_shader.bind();
        s_shader.setUniformMat4("u_transform", new Matrix4f().identity());
        s_shader.setUniformMat4("u_transform_uv", new Matrix4f().identity());
        s_shader.unbind();
    }


    public static VertexArray getDefaultVAO() {
        return s_vao;
    }

    public static Shader getDefaultShader() {
        return s_shader;
    }

    public static void renderDefaultVAO() {
        //Gcon.enableCullFace(false);

        s_vao.bind();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        s_vao.unbind();
    }

    public static void render( Texture t, FrameBuffer fbo) {
        fbo.bind();
        s_shader.bind();
        t.bind(0);
        renderDefaultVAO();
    }
}
