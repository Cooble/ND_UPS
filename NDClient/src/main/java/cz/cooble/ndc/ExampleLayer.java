package cz.cooble.ndc;

import cz.cooble.ndc.ExampleQuad;
import cz.cooble.ndc.core.App;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.graphics.*;
import cz.cooble.ndc.graphics.font.Font;
import cz.cooble.ndc.graphics.font.FontMaterial;
import cz.cooble.ndc.graphics.font.TextBuilder;
import cz.cooble.ndc.graphics.font.TextMesh;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.MousePressEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;


public class ExampleLayer extends Layer {

    private Texture m_background;


    private Texture texture;
    private VertexBuffer vbo;
    private IndexBuffer ibo;
    private VertexArray vao;
    private Shader shader;
    float[] vertices = {
            -1f, -1f, 0f,     0,0,
            1f, -1f, 0f,      1,0,
            1.f, 1f, 0.f,     1,1,
            -1f, 1f, 0f,      0,1,
    };
    int[] indices = {
            0,1,2,0,2,3
    };

    private Matrix4f modelMatrix=new Matrix4f().identity();

    private BatchRenderer2D renderer;

    private ExampleQuad quad;
    TextMesh textMesh;
    FontMaterial fontMaterial;





    @Override
    public void onAttach() {
        //vbo
        var b = FloatBuffer.wrap(vertices);
        b.flip();
        vbo = new VertexBuffer(b, VertexBuffer.Usage.STATIC_DRAW);
        vbo.setLayout(new VertexBuffer.Layout(
                new VertexBuffer.Element(g_typ.VEC3),
                new VertexBuffer.Element(g_typ.VEC2)));
        //ibo
        var e = IntBuffer.wrap(indices);
        e.flip();
        ibo = new IndexBuffer(e, VertexBuffer.Usage.STATIC_DRAW);

        //text
        texture = new Texture(new Texture.Info().path("C:\\Users\\minek\\Pictures\\Video Projects\\Capture.png"));

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
                    gl_Position = u_projectionMatrix * u_viewMatrix * u_worldMatrix * position;
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
                	color = vec4(1,1,1,1);
                }
                """;


        shader = new Shader(source);
        shader.bind();
        shader.setUniformMat4("u_worldMatrix",modelMatrix);
        shader.setUniformMat4("u_projectionMatrix",new Matrix4f().identity());
        shader.setUniformMat4("u_viewMatrix",new Matrix4f().identity());
        shader.setUniform1i("u_texture",0);
        shader.unbind();

        fontMaterial = FontMaterial.Lib.getMaterial("fonts/andrew_big_czech.fnt");
        textMesh=new TextMesh(100);

        var list = new ArrayList<String>();
        list.add("b&f&7Ahob&2&bjky");
        TextBuilder.buildMesh(list,fontMaterial.font,textMesh,TextBuilder.ALIGN_LEFT,new Vector4i(-100,-100,10000,10000),null);
        //TextBuilder.buildMesh(TextBuilder.convertToLines("&1AThh!!! WW",fontMaterial.font,1000),fontMaterial.font,textMesh,TextBuilder.ALIGN_LEFT,new Vector4i(0,0,10000,10000),null);

        // Set the clear color
        GCon.clearColor(0.0f, 0.0f, 0.0f, 1.0f);

        renderer=new BatchRenderer2D();

        quad = new ExampleQuad(
                new Vector3f(0,0,0),
                new Vector2f(1,1),
                Renderable2D.buildUV(new Vector2f(0,0),new Vector2f(1,1)),
                texture);

        m_background = new Texture(new Texture.Info("res/images/background.png"));
        frameBuffer=  new FrameBuffer(new FrameBuffer.Info(new Texture.Info().size(400,400)));

        Effect.init();
       // initeff();
    }
    VertexBuffer s_vbo;
    VertexArray s_vao;

    private void initeff(){
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
        var eee = FloatBuffer.wrap(f);
        eee.flip();
        s_vbo = new VertexBuffer(eee, VertexBuffer.Usage.STATIC_DRAW);
        var layout = new VertexBuffer.Layout(
                g_typ.VEC2, //pos
                g_typ.VEC2 //uv
        );

        s_vbo.setLayout(layout);
        s_vao = new VertexArray();

        s_vao.addBuffer(s_vbo);

       /* s_shader = Shader.Lib.getOrLoad("res/shaders/TextureQuad.shader");
        s_shader.bind();
        s_shader.setUniformMat4("u_transform", new Matrix4f().identity());
        s_shader.unbind();*/
    }

    FrameBuffer frameBuffer;
    float angle;
    float scaler = 0.001f;
    @Override
    public void onRender() {

       /* renderer.begin( Renderer.getDefaultFBO());
        renderer.submitTextureQuad(new Vector3f(-1.f, -1.f, 0.9f ), new Vector2f(2.f, 2), Renderable2D.elementary(), m_background);

        //renderer.push(new Matrix4f().rotate(angle,0,0,1));
       // renderer.submit(quad);

        renderer.push(new Matrix4f().rotate(angle,0,0,1));
        renderer.push(new Matrix4f().scale(2.f / App.get().getWindow().getWidth(), 2.f / App.get().getWindow().getHeight(), 1));

        quad.getSize().y=400;
        quad.getSize().x=400;
        renderer.submit(quad);

        renderer.submitText(textMesh,fontMaterial);

        renderer.pop();
        renderer.pop();
        renderer.pop();

        renderer.flush();*/

        //Effect.render(frameBuffer.getAttachment(),Renderer.getDefaultFBO());


       // Effect.render(m_background,Renderer.getDefaultFBO());

        shader.bind();
        //t.bind(0);
        vao.bind();
        glDrawArrays(GL_TRIANGLES, 0, 3);

        /*shader.bind();
        shader.setUniformMat4("u_worldMatrix",modelMatrix);
        texture.bind(0);
        Renderer.draw(vao,shader,ibo);*/
    }

    float angleInc=0.02f;

    @Override
    public void onUpdate() {

        angle+=angleInc;
        //System.out.println("scale "+scaler);
    }

    @Override
    public void onEvent(Event e) {
        if(e.getType()== Event.EventType.MousePress){
            var ee = (MousePressEvent) e;
            if(!ee.isRelease())
                angleInc*=-1;
        }
    }
}
