package cz.cooble.ndc.graphics;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;

public class IndexBuffer {
    private int m_id;
    private int m_count;


    public IndexBuffer(IntBuffer data, VertexBuffer.Usage usage){
        this.m_count = data.capacity();
        m_id= glGenBuffers();
        bind();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,data,usage.i);
        unbind();
    }
    public IndexBuffer(int[] data, VertexBuffer.Usage usage){
        this.m_count = data.length;
        m_id= glGenBuffers();
        bind();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,data,usage.i);
        unbind();
    }

    public void bind(){
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,m_id);
    }
    public void unbind(){
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,0);
    }

    public int getCount() {
        return m_count;
    }
    public void delete(){
        glDeleteBuffers(m_id);
    }
    ByteBuffer mapPointer() {
        ByteBuffer out = glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY);
        if(out==null)
            throw new RuntimeException("[OpenGLVertexBuffer]: Cannot get map pointer to VBO, is it currently bound?");
        return out;
    }

    void unMapPointer() {
        glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
    }

    void changeData(ByteBuffer buff, long offset) {
        bind();
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, offset, buff);
    }
}
