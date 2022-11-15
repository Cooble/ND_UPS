package cz.cooble.ndc.test;

import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.graphics.*;
import org.joml.Matrix4f;

import static cz.cooble.ndc.graphics.GUtils.wrapBuffer;


public class TestTexture extends Layer {

    private Texture texture;

    private VertexBuffer vbo;
    private IndexBuffer ibo;
    private VertexArray vao;
    private Shader shader;
    float[] vertices = {
            -1f, -1f, 0f, 0, 0,
            1f, -1f, 0f, 1, 0,
            1.f, 1f, 0.f, 1, 1,
            -1f, 1f, 0f, 0, 1,
    };
    int[] indices = {
            0, 1, 2, 0, 2, 3
    };

    @Override
    public void onAttach() {
        //vbo
        vbo = new VertexBuffer(wrapBuffer(vertices), VertexBuffer.Usage.STATIC_DRAW);
        vbo.setLayout(new VertexBuffer.Layout(
                new VertexBuffer.Element(g_typ.VEC3),
                new VertexBuffer.Element(g_typ.VEC2)));
        //ibo
        ibo = new IndexBuffer(wrapBuffer(indices), VertexBuffer.Usage.STATIC_DRAW);

        //vao
        vao = new VertexArray();
        vao.addBuffer(vbo);
        vao.addBuffer(ibo);

        Shader.Source source = new Shader.Source();

        source.vertex = """
                #version 330 core
                layout (location = 0) in vec4 position;
                layout (location = 1) in vec2 uv;

                out vec2 v_uv;
                                
                uniform mat4 u_worldMatrix;
                uniform mat4 u_projectionMatrix;
                uniform mat4 u_viewMatrix;
                                
                void main()
                {
                    gl_Position = u_worldMatrix * vec4(position.xyz,1);
                    v_uv = uv;
                }
                """;

        source.fragment = """
                #version 330 core
                                
                uniform sampler2D u_texture;
                                
                layout(location=0) out vec4 color;
                                
                in vec2 v_uv;
                                
                void main(){
                	vec4 cc = texture2D(u_texture, v_uv);
                	color = cc;
                }
                """;


        shader = new Shader(source);
        shader.bind();
        shader.setUniform1i("u_texture", 0);
        shader.setUniformMat4("u_worldMatrix", new Matrix4f().identity());

        shader.unbind();

        GCon.clearColor(0.0f, 0.0f, 0.0f, 1.0f);

        texture = new Texture(new Texture.Info("res/images/background.png"));

    }

    @Override
    public void onRender() {
        vao.bind();
        ibo.bind();
        shader.bind();
        texture.bind(0);
        GCon.cmdDrawElements(GCon.Topology.TRIANGLES, 6);
    }
}
