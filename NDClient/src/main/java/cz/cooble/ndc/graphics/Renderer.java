package cz.cooble.ndc.graphics;

public class Renderer {

    private static FrameBuffer s_default_fbo;


    public static void setDefaultFBO(FrameBuffer fbo) {
        Renderer.s_default_fbo = fbo;
    }

    public static FrameBuffer getDefaultFBO() {
        return s_default_fbo;
    }

    public static void draw(VertexArray vao, Shader shader, IndexBuffer ibo) {
        vao.bind();
        shader.bind();
        ibo.bind();

        GCon.cmdDrawElements(GCon.Topology.TRIANGLES, ibo.getCount());
    }

    public static void clear() {
        GCon.clear(GCon.BufferBit.COLOR | GCon.BufferBit.DEPTH); // clear the framebuffer
    }
}

