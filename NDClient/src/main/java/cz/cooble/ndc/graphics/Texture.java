package cz.cooble.ndc.graphics;

import cz.cooble.ndc.core.IOUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    public int id() {
        return m_id;
    }

    public Info getInfo() {
        return m_info;
    }

    public int width() {
        return m_width;
    }

    public int height() {
        return m_height;
    }

    public enum WrapMode {
        REPEAT(GL_REPEAT),
        CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE),
        CLAMP_TO_BORDER(GL_CLAMP_TO_BORDER);


        final int i;

        WrapMode(int i) {
            this.i = i;
        }
    }

    public enum FilterMode {
        NEAREST(GL_NEAREST),
        LINEAR(GL_LINEAR);
        final int i;

        FilterMode(int i) {
            this.i = i;
        }
    }

    public enum Format {
        RGBA(GL_RGBA),
        RGB(GL_RGB),
        RED(GL_RED);

        final int i;

        Format(int i) {
            this.i = i;
        }
    }

    enum TextureType {
        _2D(GL_TEXTURE_2D),
        _CUBE_MAP(GL_TEXTURE_CUBE_MAP),
        _3D(GL_TEXTURE_3D);

        final int i;

        TextureType(int i) {
            this.i = i;
        }
    }


    public static class Info {
        float[] border_color = {0, 0, 0, 0}; //transparent color outside of image
        int width = 0, height = 0;
        String file_path="";
        FilterMode filter_mode_min = FilterMode.LINEAR;
        FilterMode filter_mode_max = FilterMode.LINEAR;
        TextureType texture_type = TextureType._2D;
        WrapMode wrap_mode_s = WrapMode.REPEAT;
        WrapMode wrap_mode_t = WrapMode.REPEAT;
        WrapMode wrap_mode_r = WrapMode.REPEAT;
        Format f_format = Format.RGBA;

        ByteBuffer pixel_data = null;

        public Info filterMode(FilterMode mode) {
            filter_mode_min = mode;
            filter_mode_max = mode;
            return this;
        }

        public Info wrapMode(WrapMode mode) {
            wrap_mode_s = mode;
            wrap_mode_t = mode;
            wrap_mode_r = mode;
            return this;
        }

        public Info path(String s) {
            file_path = s;
            return this;
        }

        public Info size(int w, int h) {
            width = w;
            height = h;
            return this;
        }

        public Info size(int d) {
            width = d;
            height = d;
            return this;
        }

        public Info format(Format form) {
            f_format = form;
            return this;
        }

        public Info pixels(ByteBuffer pixels) {
            this.pixel_data = pixels;
            return this;
        }

        public Info borderColor(float r, float g, float b, float a) {
            border_color[0] = r;
            border_color[1] = g;
            border_color[2] = b;
            border_color[3] = a;
            return this;
        }

        public Info type(TextureType type) {
            texture_type = type;

            return this;
        }

        public Info() {}
        public Info(String string) {
            file_path = string;
        }

        public Info(TextureType type, String string) {
            texture_type = type;
            if (type == TextureType._CUBE_MAP) {
                wrapMode(WrapMode.CLAMP_TO_EDGE);
                format(Format.RGB);
            }
            file_path = string;
        }
    }

    int m_id;
    int m_BPP; //channels
    ByteBuffer m_buffer;
    int m_width, m_height;
    TextureType m_type;
    Format m_format;
    String m_filePath;
    Info m_info;

    public Texture(Info info) {
        this.m_width = info.width;
        this.m_height = info.height;
        this.m_type = info.texture_type;
        this.m_format = info.f_format;
        this.m_filePath = info.file_path;
        this.m_info = info;

        var type = info.texture_type.i;

        m_id = glGenTextures();

        glBindTexture(type, m_id);
        glTexParameteri(type, GL_TEXTURE_MIN_FILTER, info.filter_mode_min.i);
        glTexParameteri(type, GL_TEXTURE_MAG_FILTER, info.filter_mode_max.i);
        glTexParameteri(type, GL_TEXTURE_WRAP_S, info.wrap_mode_s.i);
        glTexParameteri(type, GL_TEXTURE_WRAP_T, info.wrap_mode_t.i);
        if (info.texture_type == TextureType._CUBE_MAP)
            glTexParameteri(type, GL_TEXTURE_WRAP_R, info.wrap_mode_r.i);


        if (info.wrap_mode_s == WrapMode.CLAMP_TO_BORDER || info.wrap_mode_t == WrapMode.CLAMP_TO_BORDER)
            glTexParameterfv(type, GL_TEXTURE_BORDER_COLOR, info.border_color);

        if (!info.file_path.isEmpty()) {
            stbi_set_flip_vertically_on_load(true);
            if (info.texture_type == TextureType._2D) {

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w    = stack.mallocInt(1);
                    IntBuffer h    = stack.mallocInt(1);
                    IntBuffer comp = stack.mallocInt(1);

                    // Decode the image
                    m_buffer = stbi_load_from_memory(IOUtils.ioResourceToByteBuffer(m_filePath, 3_000_000), w, h, comp, 0);
                    if (m_buffer == null) {
                        throw new RuntimeException("Failed to load image: " + stbi_failure_reason());
                    }
                    this.m_width = w.get();
                    this.m_height = h.get();
                    this.m_BPP = comp.get();

                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }
                glTexImage2D(GL_TEXTURE_2D, 0, m_format.i, m_width, m_height, 0, m_format.i, GL_UNSIGNED_BYTE, m_buffer);
                if (m_buffer!=null) {
                 //   stbi_image_free(m_buffer); todo should i free it? stbi
                }
                else {
                    System.err.println("Invalid texture path: {}"+ m_filePath);
                   // ND_ERROR("Invalid texture path: {}", m_filePath.c_str());
                }
            } else if (info.texture_type == TextureType._CUBE_MAP) {
                System.err.println("Unsupported _CUBE_MAP");
            }
        }
        else if (info.pixel_data!=null) {
                    glTexImage2D(GL_TEXTURE_2D, 0, m_format.i, m_width, m_height, 0,m_format.i, GL_UNSIGNED_BYTE, info.pixel_data);
        } else {
            // resolves problem with weird alignment of pixels
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            var fixer = m_format == Format.RGB ? GL_RGB8 : m_format.i;
            glTexImage2D(GL_TEXTURE_2D, 0, fixer, m_width, m_height, 0,  m_format.i, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        }
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    public void bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(m_type.i, m_id);
    }

    public void unbind() {
        glBindTexture(m_type.i, 0);
    }

    public void delete(){
        glDeleteTextures(m_id);
    }

    public void setPixels(float[] pixels)
    {
        //ASSERT(m_not_proxy, "This texture is only proxy");
        glBindTexture(m_type.i, m_id);
        glTexSubImage2D(m_type.i, 0, 0, 0, m_width, m_height, m_format.i, GL_FLOAT, pixels);
        glBindTexture(m_type.i, 0);
    }

    public void setPixels(ByteBuffer pixels)
    {
        //ASSERT(m_not_proxy, "This texture is only proxy");
        glBindTexture(m_type.i, m_id);
        glTexSubImage2D(m_type.i, 0, 0, 0, m_width, m_height, m_format.i, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(m_type.i, 0);
    }
}
