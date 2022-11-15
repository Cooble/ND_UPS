package cz.cooble.ndc.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.core.IOUtils.readFile;
import static org.lwjgl.opengl.GL20.*;

public class Shader {
    public int id() {
        return m_id;
    }

    public static class Source {
        public String vertex;
        public String fragment;
    }

    private final int m_id;
    private boolean m_isBound;
    private int vertexShaderId;
    private int fragmentShaderId;

    void checkIsBound()
    {
        if (!m_isBound)
            throw new RuntimeException("Cannot set uniform when no shader is bound");
    }
    
    public Shader(Source sources) {
        m_id = glCreateProgram();
        if (m_id == 0)
            throw new RuntimeException("Could not create Program");
        try{
            vertexShaderId = createShader(sources.vertex,GL_VERTEX_SHADER);
            fragmentShaderId = createShader(sources.fragment,GL_FRAGMENT_SHADER);
        }catch (RuntimeException e){
            if(vertexShaderId!=0)
                glDeleteShader(vertexShaderId);

            glDeleteProgram(m_id);
            throw e;
        }
        link();
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }
    protected int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        ASSERT(shaderId != 0,"Error creating shader. Type: " + shaderType);

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        ASSERT(glGetShaderi(shaderId, GL_COMPILE_STATUS) != 0,"Error compiling Shader code: " + glGetShaderInfoLog(shaderId, 1024));

        glAttachShader(m_id, shaderId);
        return shaderId;
    }

    private void link() {
        glLinkProgram(m_id);
        ASSERT(glGetProgrami(m_id, GL_LINK_STATUS) != 0,"Error linking Shader code: " + glGetProgramInfoLog(m_id, 1024));

        if (vertexShaderId != 0)
            glDetachShader(m_id, vertexShaderId);
        if (fragmentShaderId != 0)
            glDetachShader(m_id, fragmentShaderId);

        glValidateProgram(m_id);
        if (glGetProgrami(m_id, GL_VALIDATE_STATUS) == 0)
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(m_id, 1024));

    }

    public void bind() {
        m_isBound=true;
        glUseProgram(m_id);
    }

    public void unbind() {
        m_isBound=false;
        glUseProgram(0);
    }

    public void delete() {
        unbind();
        if (m_id != 0) {
            glDeleteProgram(m_id);
        }
    }
    public void setUniformMat4(String name, Matrix4f matrix)
    {
        checkIsBound();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = matrix.get(stack.mallocFloat(16));
            glUniformMatrix4fv(getUniformLocation(name), false, fb);
        }
    }

    public void setUniformMat3(String name, Matrix4f matrix)
    {
        checkIsBound();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = matrix.get(stack.mallocFloat(9));
            glUniformMatrix4fv(getUniformLocation(name), false, fb);
        }
    }

    public void setUniform1iv(String name,int[] i)
    {
        checkIsBound();
        glUniform1iv(getUniformLocation(name),i);
    }

    public void setUniform4f(String name, float f0, float f1, float f2, float f3) {
        checkIsBound();
        glUniform4f(getUniformLocation(name), f0, f1, f2, f3);
    }

    public void setUniformVec4f(String name, Vector4f vec) {
        setUniform4f(name, vec.x, vec.y, vec.z, vec.w);
    }

    public void setUniformVec3f(String name, Vector3f vec) {
        setUniform3f(name, vec.x, vec.y, vec.z);
    }

    public void setUniform3f(String name, float f0, float f1, float f2) {
        checkIsBound();
        glUniform3f(getUniformLocation(name), f0, f1, f2);
    }


    public void setUniform1f(String name, float f0) {
        checkIsBound();
        glUniform1f(getUniformLocation(name), f0);
    }

    public void setUniform2f(String name, float f0, float f1) {
        checkIsBound();
        glUniform2f(getUniformLocation(name), f0, f1);
    }

    public void setUniform1i(String name, int v) {
        checkIsBound();
        glUniform1i(getUniformLocation(name), v);
    }

    public int getUniformLocation(String name)
    {
        var out =glGetUniformLocation(m_id, name);
        ASSERT(out!=-1,"Invalid uniform name: "+name);
        return out;
    }

    public static class Lib {

        public static Shader getOrLoad(String location){
            Source s = new Source();
            String shaderSource = readFile(location);
            shaderSource = shaderSource.replace("#shader vertex","");
            var split = shaderSource.split("#shader fragment");

            ASSERT(split.length==2,"Invalid shader "+location);

            s.vertex=split[0];
            s.fragment=split[1];

            return new Shader(s);
        }
    }
}
