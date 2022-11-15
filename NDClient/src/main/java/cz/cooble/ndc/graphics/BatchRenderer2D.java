package cz.cooble.ndc.graphics;

import cz.cooble.ndc.graphics.font.FontMaterial;
import cz.cooble.ndc.graphics.font.TextMesh;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.*;

import static cz.cooble.ndc.Asserter.ASSERT;

public class BatchRenderer2D {
    private static final int MAX_TEXTURES = 16;
    private static final int MAX_QUADS = 5000;
    private static final int MAX_VERTICES = MAX_QUADS * 4;
    private static final int MAX_FLOATS = MAX_VERTICES * (3 + 2 + 1 + 1);

    private static final int MAX_INDICES = MAX_QUADS * 6;

    private final List<Matrix4f> m_transformation_stack = new ArrayList<>();
    private Matrix4f m_back;

    private final FloatBuffer m_buff;
    private final FloatBuffer m_text_buff;
    private Shader m_shader;
    private VertexBuffer m_vbo;
    private VertexArray m_vao;
    private IndexBuffer m_ibo;
    private final List<Texture> m_textures = new ArrayList<>();
    private FrameBuffer m_fbo;
    private int m_indices_count;
    private boolean m_apply_default_blending = true;
    private int m_text_indices_count;
    private VertexBuffer m_text_vbo;
    private Shader m_text_shader;
    private VertexArray m_text_vao;


    private static class TextIBOView {
       int fromIndex;
       int length;
        private TextIBOView(int fromIndex, int length) {
            this.fromIndex = fromIndex;
            this.length = length;
        }
    }

    Map<FontMaterial, List<TextIBOView>> m_fonts = new HashMap<>();


    public void setDefaultBlending(boolean m_apply_default_blending) {
        this.m_apply_default_blending = m_apply_default_blending;
    }

    public BatchRenderer2D() {
        m_transformation_stack.add(new Matrix4f().identity());
        m_back = m_transformation_stack.get(0);

        m_buff = BufferUtils.createFloatBuffer(MAX_FLOATS);
        m_text_buff = BufferUtils.createFloatBuffer(MAX_FLOATS);

        prepareQuad();
        prepareText();
    }

    void prepareQuad() {
        var uniforms = new int[MAX_TEXTURES];
        for (int i = 0; i < MAX_TEXTURES; ++i)
            uniforms[i] = i;

        m_shader = Shader.Lib.getOrLoad("res/shaders/Sprite.shader");
        m_shader.bind();
        m_shader.setUniform1iv("u_textures", uniforms);
        m_shader.setUniformMat4("u_projectionMatrix", new Matrix4f().identity());
        m_shader.unbind();

        var l = new VertexBuffer.Layout(
                g_typ.VEC3,//POS
                g_typ.VEC2,//UV
                g_typ.UNSIGNED_INT,//TEXTURE_SLOT
                g_typ.UNSIGNED_INT);//COLOR


        m_vbo = new VertexBuffer(MAX_FLOATS * 4, VertexBuffer.Usage.STREAM_DRAW);
        m_vbo.setLayout(l);

        m_vao = new VertexArray();
        m_vao.addBuffer(m_vbo);

        var indices = new int[MAX_INDICES];
        int offset = 0;
        for (int i = 0; i < MAX_INDICES; i += 6) {
            indices[i + 0] = offset + 0;
            indices[i + 1] = offset + 1;
            indices[i + 2] = offset + 2;

            indices[i + 3] = offset + 0;
            indices[i + 4] = offset + 2;
            indices[i + 5] = offset + 3;

            offset += 4;
        }
        m_ibo = new IndexBuffer(indices, VertexBuffer.Usage.STATIC_DRAW);
    }

    void prepareText() {

        m_text_shader = Shader.Lib.getOrLoad("res/shaders/Font.shader");
        m_text_shader.bind();
        m_text_shader.setUniform1i("u_texture", 0);
        m_text_shader.setUniformMat4("u_transform", new Matrix4f().identity());
        m_text_shader.unbind();

        var l = new VertexBuffer.Layout (
        g_typ.VEC3, //POS
                g_typ.VEC2, //UV
                g_typ.UNSIGNED_INT, //COLOR
                g_typ.UNSIGNED_INT //BORDER_COLOR

        );

        m_text_vbo = new VertexBuffer(MAX_FLOATS * 4, VertexBuffer.Usage.STREAM_DRAW);
        m_text_vbo.setLayout(l);

        m_text_vao = new VertexArray();
        m_text_vao.addBuffer(m_text_vbo);
    }


    public void delete() {
        m_vbo.delete();
        m_vao.delete();
        m_ibo.delete();
        m_shader.delete();

        m_text_vbo.delete();
        m_text_vao.delete();
        m_text_shader.delete();
    }

    public void push(Matrix4f trans) {
        ASSERT(m_transformation_stack.size() < 500, "Somebody forget to call pop()..");
        m_transformation_stack.add(new Matrix4f(m_back).mul(trans));
        m_back = m_transformation_stack.get(m_transformation_stack.size() - 1);
    }

    public void pop(int count) {
        for (int i = 0; i < count && m_transformation_stack.size() > 1; ++i)
            m_transformation_stack.remove(m_transformation_stack.size() - 1);

        m_back = m_transformation_stack.get(m_transformation_stack.size() - 1);
    }

    public void pop() {
        pop(1);
    }

    private int bindTexture(Texture t) {
        for (int i = 0; i < m_textures.size(); ++i)
            if (t == m_textures.get(i))
                return i;

        if (m_textures.size() != MAX_TEXTURES) {
            m_textures.add(t);
            return m_textures.size() - 1;
        }

        flushQuad();
        beginQuad();
        return bindTexture(t);
    }

    public void begin(FrameBuffer fbo) {
        m_fbo = fbo;
        beginQuad();
        beginText();
    }

    private void beginText() {
       m_text_indices_count = 0;
       m_text_buff.clear();
    }

    static int BUF_S = 500;
    static int[] lengths = new int[BUF_S];
    static int[] offsets = new int[BUF_S];


    private void flushText() {
        if (m_text_indices_count == 0)
            return;

        m_text_vbo.bind();
        m_text_buff.flip();
        m_text_vbo.changeData(m_text_buff, 0);

        m_text_vao.bind();
        m_ibo.bind();
        m_text_shader.bind();

        if (m_apply_default_blending)
        {
            GCon.enableBlend();
            GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
        }
        Arrays.fill(lengths,0);
        Arrays.fill(offsets,0);

        for (var fontMat : m_fonts.entrySet())
        {
            var views = fontMat.getValue();

            if (views.isEmpty())
                continue;
            fontMat.getKey().texture.bind(0);

            //std.static_pointer_cast<nd.internal.GLShader>(m_text_shader).setUniformVec4f("u_textColor", fontMat.first.color);
            //std.static_pointer_cast<nd.internal.GLShader>(m_text_shader).setUniformVec4f("u_borderColor", fontMat.first.border_color);

            if (views.size() > BUF_S) {
                BUF_S = views.size();
                lengths = new int[BUF_S];
                offsets = new int[BUF_S];
            }

            for (int i = 0; i < views.size(); ++i) {
                lengths[i] = views.get(i).length;
                offsets[i] = views.get(i).fromIndex;
            }
            GCon.cmdDrawMultiElements(GCon.Topology.TRIANGLES, offsets, lengths,views.size());
            views.clear();
        }
    }

    private void beginQuad() {
        m_indices_count = 0;
        m_textures.clear();
        m_buff.clear();
    }

    private void flushQuad() {
        if (m_indices_count == 0)
            return;
        m_vbo.bind();
        m_buff.flip();
        m_vbo.changeData(m_buff, 0);

        m_vao.bind();
        m_ibo.bind();
        m_shader.bind();

        for (int i = 0; i < m_textures.size(); ++i)
            m_textures.get(i).bind(i);

        if (m_apply_default_blending) {
            GCon.enableBlend();
            GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
        }
        GCon.cmdDrawElements(GCon.Topology.TRIANGLES, m_indices_count);
    }

    static Vector3f mult4w3(Matrix4f m, Vector3f v) {
        Vector4f inV = new Vector4f(v.x, v.y, v.z, 1);
        inV = inV.mul(m);
        return new Vector3f(inV.x, inV.y, inV.z);
    }

    private void buffMe(Vector3f p) {
        m_buff.put(p.x);
        m_buff.put(p.y);
        m_buff.put(p.z);
    }
    private void buffMe(int p) {
        m_buff.put(Float.intBitsToFloat(p));
    }

    private void buffMe(Vector2f uv) {
        m_buff.put(uv.x);
        m_buff.put(uv.y);
    }

    private void buffMeText(Vector3f p) {
        m_text_buff.put(p.x);
        m_text_buff.put(p.y);
        m_text_buff.put(p.z);
    }
    private void buffMeText(int p) {
        m_text_buff.put(Float.intBitsToFloat(p));
    }
    private void buffMeText(Vector2f uv) {
        m_text_buff.put(uv.x);
        m_text_buff.put(uv.y);
    }

    public void submit(Renderable2D renderable) {
        var pos = renderable.getPosition();
        var size = renderable.getSize();
        var uv = renderable.getUV();

        submitTextureQuad(pos, size, uv, renderable.getTexture(), 1);
    }
    public void submitTextureQuad(Vector3f pos, Vector2f size, Vector2f[] uv, Texture t) {
        submitTextureQuad(pos,size,uv,t,1);
    }
    public void submitTextureQuad(Vector3f pos, Vector2f size, Vector2f[] uv, Texture t, float alpha) {
        int textureSlot = bindTexture(t);
        var colo = ((int) (alpha * 255) << 24);


        buffMe(mult4w3(m_back, pos));
        buffMe(uv[0]);
        buffMe(textureSlot);
        buffMe(colo);

        buffMe(mult4w3(m_back, new Vector3f(pos).add(size.x, 0, 0)));
        buffMe(uv[1]);
        buffMe(textureSlot);
        buffMe(colo);

        buffMe(mult4w3(m_back, new Vector3f(pos).add(size.x, size.y, 0)));
        buffMe(uv[2]);
        buffMe(textureSlot);
        buffMe(colo);

        buffMe(mult4w3(m_back, new Vector3f(pos).add(0, size.y, 0)));
        buffMe(uv[3]);
        buffMe(textureSlot);
        buffMe(colo);

        m_indices_count += 6;
        if (m_indices_count == MAX_INDICES) {
            flush();
            begin(m_fbo);
        }
    }

    private void buff2Zeros() {
        m_buff.put(0);
        m_buff.put(0);
    }

    public void submitColorQuad(Vector3f pos, Vector2f size, Vector4f color) {
        int col = (
                ((int) (color.x * 255) << 0) |
                        ((int) (color.y * 255) << 8) |
                        ((int) (color.z * 255) << 16) |
                        ((int) (color.w * 255) << 24));

        buffMe(mult4w3(m_back, pos));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        buffMe(mult4w3(m_back, new Vector3f(pos).add(size.x, 0, 0)));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        buffMe(mult4w3(m_back, new Vector3f(pos).add(size.x, size.y, 0)));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        buffMe(mult4w3(m_back, new Vector3f(pos).add(0, size.y, 0)));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        m_indices_count += 6;
        if (m_indices_count == MAX_INDICES) {
            flushQuad();
            beginQuad();
        }
    }

    public void submitColorTriangle(Vector2f pos0, Vector2f pos1, Vector2f pos2, float z, Vector4f color) {
        int col = (
                ((int) (color.x * 255) << 0) |
                        ((int) (color.y * 255) << 8) |
                        ((int) (color.z * 255) << 16) |
                        ((int) (color.w * 255) << 24));

        buffMe(mult4w3(m_back, new Vector3f(pos0,z)));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        buffMe(mult4w3(m_back, new Vector3f(pos1,z)));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        buffMe(mult4w3(m_back, new Vector3f(pos2,z)));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        buffMe(mult4w3(m_back, new Vector3f(pos2,z)));
        buff2Zeros();
        buffMe(Integer.MAX_VALUE);
        buffMe(col);

        m_indices_count += 6;
        if (m_indices_count == MAX_INDICES) {
            flushQuad();
            beginQuad();
        }
    }

    public void submitText(TextMesh mesh, FontMaterial material) {
        if (mesh.getVertexCount() == 0)
            return;
        var data = mesh.getSrc();

        if (m_text_indices_count + mesh.currentCharCount * 6 >= MAX_INDICES) {
            flushText();
            beginText();
        }
        m_fonts.computeIfAbsent(material, fontMaterial -> new ArrayList<>()).add(new TextIBOView(m_text_indices_count, mesh.currentCharCount * 6));
        for (int i = 0; i < mesh.currentCharCount; ++i) {
            var ch = data[i];
            var v00 = ch.v[0];
            var v01 = ch.v[1];
            var v02 = ch.v[2];
            var v03 = ch.v[3];


            var v0 = new Vector3f(v00.pos.x, v00.pos.y, 0);
            var v1 = new Vector3f(v01.pos.x, v01.pos.y, 0);
            var v2 = new Vector3f(v02.pos.x, v02.pos.y, 0);
            var v3 = new Vector3f(v03.pos.x, v03.pos.y, 0);

            v0 = mult4w3(m_back,v0);
            //v1 = m_back * v1;
            v2 = mult4w3(m_back,v2);
            //v3 = m_back * v3;

            v1 = new Vector3f(v2.x, v0.y, v0.z);
            v3 = new Vector3f(v0.x, v2.y, v0.z);


            buffMeText(v0);
            buffMeText(v00.uv);
            buffMeText(v00.color);
            buffMeText(v00.borderColor);

            buffMeText(v1);
            buffMeText(v01.uv);
            buffMeText(v00.color);
            buffMeText(v00.borderColor);

            buffMeText(v2);
            buffMeText(v02.uv);
            buffMeText(v00.color);
            buffMeText(v00.borderColor);

            buffMeText(v3);
            buffMeText(v03.uv);
            buffMeText(v00.color);
            buffMeText(v00.borderColor);

            m_text_indices_count += 6;
        }
    }

    public void flush() {
        m_fbo.bind();
        flushQuad();
        flushText();
    }
}
