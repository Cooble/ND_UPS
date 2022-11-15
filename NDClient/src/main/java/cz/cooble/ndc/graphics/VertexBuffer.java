package cz.cooble.ndc.graphics;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static cz.cooble.ndc.graphics.GUtils.wrapBuffer;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBufferSubData;

public class VertexBuffer {

    public int id() {
        return m_id;
    }

    public static class Element {
        g_typ typ;
        boolean normalized;
        int count;

        public Element() {
        }

        public Element(g_typ typ) {
            this.typ = typ;
            this.count = 1;
            this.normalized = false;
        }

        public Element(g_typ typ, int count, boolean norm) {
            this.typ = typ;
            this.count = count;
            this.normalized = norm;
        }
    }

    public static class Layout {

        private final List<Element> m_elements;
        private int m_stride;

        public Layout(Element... list) {
            m_elements = new ArrayList<>();
            for (var e : list)
                pushElement(e);
        }
        public Layout(g_typ... list) {
            m_elements = new ArrayList<>();
            for (var e : list)
                pushElement(new Element(e));
        }

        public void pushElement(Element e) {
            m_elements.add(e);
            m_stride += GCon.GTypes.getSize(e.typ) * e.count;
        }

        public int getStride() {
            return m_stride;
        }

        public List<Element> getElements() {
            return m_elements;
        }
    }

    public enum Usage {
        STATIC_DRAW(GL_STATIC_DRAW),
        DYNAMIC_DRAW(GL_DYNAMIC_DRAW),
        STREAM_DRAW(GL_STREAM_DRAW);

        public final int  i;
        Usage(int i) {
            this.i = i;
        }
    }

    private int m_id;
    private int m_size;

    private Layout layout;


    public VertexBuffer(int size, Usage usage) {
        this.m_size = size;
        m_id = glGenBuffers();
        bind();
        glBufferData(GL_ARRAY_BUFFER, size, usage.i);
        unbind();
    }
    public VertexBuffer(ByteBuffer byteBuffer, Usage usage) {
        this.m_size = byteBuffer.capacity();
        m_id = glGenBuffers();
        bind();
        glBufferData(GL_ARRAY_BUFFER, byteBuffer, usage.i);
        unbind();
    }
    public VertexBuffer(FloatBuffer byteBuffer, Usage usage) {
        this.m_size = byteBuffer.capacity();
        m_id = glGenBuffers();
        bind();
        glBufferData(GL_ARRAY_BUFFER, byteBuffer, usage.i);
        unbind();
    }

    public void bind() {
        glBindBuffer(GL_ARRAY_BUFFER, m_id);
    }

    public void unbind() {
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public int getSize() {
        return m_size;
    }

    public void delete() {
        glDeleteBuffers(m_id);
    }

    public ByteBuffer mapPointer() {
        ByteBuffer out = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY);
        if (out == null)
            throw new RuntimeException("[OpenGLVertexBuffer]: Cannot get map pointer to VBO, is it currently bound?");
        return out;
    }

    public void unMapPointer() {
        glUnmapBuffer(GL_ARRAY_BUFFER);
    }

    public void changeData(ByteBuffer buff, long byteOffset) {
        bind();
        glBufferSubData(GL_ARRAY_BUFFER, byteOffset, buff);
    }
    public void changeData(FloatBuffer buff, long byteOffset) {
        bind();
        glBufferSubData(GL_ARRAY_BUFFER, byteOffset, buff);
    }
    public void changeData(float[] buff,long byteOffset) {
        bind();
        glBufferSubData(GL_ARRAY_BUFFER,byteOffset,wrapBuffer(buff));
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public Layout getLayout() {
        return layout;
    }
}
