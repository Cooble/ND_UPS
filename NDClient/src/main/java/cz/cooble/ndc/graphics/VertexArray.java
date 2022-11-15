package cz.cooble.ndc.graphics;


import static org.lwjgl.opengl.GL30.*;
import static cz.cooble.ndc.graphics.GCon.*;

public class VertexArray {

    private final int m_id;
    private int m_atrib_point_index;

    public void addBuffer(VertexBuffer vbo) {
        bind();
        vbo.bind();
        long offset = 0;
        var ray = vbo.getLayout().getElements();

        for (int i = 0; i < ray.size(); i++) {
            var e = ray.get(i);

            glEnableVertexAttribArray(m_atrib_point_index + i);
            if (GTypes.isIType(e.typ)) {
                glVertexAttribIPointer(
                        m_atrib_point_index + i,
                        GTypes.getCount(e.typ),
                        GCon.toGL(GTypes.getBase(e.typ)),
                        vbo.getLayout().getStride(),
                        offset);
            } else {
                glVertexAttribPointer(
                        m_atrib_point_index + i,
                        GTypes.getCount(e.typ),
                        toGL(GTypes.getBase(e.typ)),
                        e.normalized,
                        vbo.getLayout().getStride(),
                        offset);
            }
            offset += GCon.GTypes.getSize(e.typ);
        }
        m_atrib_point_index += ray.size();
        unbind();
    }

    public void addBuffer(IndexBuffer vio) {
        bind();
        vio.bind();
        unbind();
    }

    public VertexArray() {
        m_id = glGenVertexArrays();
    }

    public void delete() {
        glDeleteVertexArrays(m_id);
    }

    public void bind() {
        glBindVertexArray(m_id);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    public int id() {
        return m_id;
    }
}
