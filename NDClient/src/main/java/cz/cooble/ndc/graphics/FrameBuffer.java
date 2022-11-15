package cz.cooble.ndc.graphics;

import cz.cooble.ndc.core.App;
import org.joml.Vector2i;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static cz.cooble.ndc.Asserter.ASSERT;
import static org.lwjgl.opengl.GL30.*;

public class FrameBuffer {
    public enum FBType {
        // have its own internal texture attachments
        // resize method can be called
        NORMAL_TARGET(0),
        // represent only husk which external textures need to be attached to
        // since it does not own attachments,
        //					- resize method cannot be used
        //					- viewport needs to be called manually
        // NOTE: Attached textures are not deleted on destruction!
        NORMAL_TARGET_EXTERNAL_TEXTURES(1),
        // its target is application window
        WINDOW_TARGET(2);

        public final int i;

        FBType(int i) {
            this.i = i;
        }
    }

    ;

    enum FBAttachment {
        NONE(0),
        COLOR(1),
        DEPTH_STENCIL(2),
        DEPTH_STENCIL_ACCESSIBLE(3);

        public final int i;

        FBAttachment(int i) {
            this.i = i;
        }
    }

    ;


    public static class Info {
        //type of fbo
        public FBType type = FBType.NORMAL_TARGET;

        //color attachment specifications
        public Texture.Info[] textureInfos;


        //special attachment like depth or stencil
        //FBAttachment.COLOR cannot be used
        public FBAttachment specialAttachment = FBAttachment.NONE;

        // sampling of depth buffer
        // 1 means no multisample
        public int multiSampleLevel = 1;

        //just for syntactic sugar
        //is completely ignored by FrameBuffer
        //usually Texture.Infos points to this
        public Texture.Info singleTextureInfo;

        public Info(Texture.Info info) {
            defaultTarget(info);
        }

        // dimensions CAN be zero!
        // its possible to prepare fbo with unknown dimensions
        // you just need to call resize() (which will implicitly create attachments)
        public Info(int width, int height, Texture.Format format) {
            defaultTarget(width, height, format);
        }

        public Info() {
            externalTarget();
        }

        public Info multiSample(int sampleLevel) {
            multiSampleLevel = sampleLevel;
            return this;
        }

        public Info special(FBAttachment special) {
            specialAttachment = special;
            return this;
        }

        public Info windowTarget() {
            type = FBType.WINDOW_TARGET;
            return this;
        }

        public Info externalTarget() {
            type = FBType.NORMAL_TARGET_EXTERNAL_TEXTURES;
            return this;
        }

        public Info defaultTarget(int width, int height, Texture.Format format) {
            type = FBType.NORMAL_TARGET;

            singleTextureInfo.size(width, height);
            singleTextureInfo.format(format);
            this.textureInfos = new Texture.Info[]{singleTextureInfo};
            return this;
        }

        // dimensions CAN be zero!
        // its possible to prepare fbo with unknown dimensions
        // you just need to call resize() (which will implicitly create attachments)
        public Info defaultTarget(Texture.Info info) {
            type = FBType.NORMAL_TARGET;
            singleTextureInfo = info;
            this.textureInfos = new Texture.Info[]{singleTextureInfo};
            return this;
        }

    }

    static class ColorAttachment {
        public Texture texture;

        public ColorAttachment(Texture texture) {
            this.texture = texture;
        }
    }

    Texture texture;
    static FrameBuffer s_currently_bound;
    int m_id;
    FBType m_type;

    List<ColorAttachment> m_attachments = new ArrayList<>();
    FBAttachment m_special_attachment;
    int m_special_attachment_id;
    Vector2i m_dimensions = new Vector2i();
    int multiSampleLevel = 0;

    public void bindColorAttachments() {
        int id_offset = 0;
        for (var attachment : m_attachments)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + (id_offset++), GL_TEXTURE_2D, attachment.texture.id(), 0);

        //specifies specific color attachments to be drawn into
        int maxAttachments = 16;

        ASSERT(m_attachments.size() <= maxAttachments, "Too many attachments");
        int[] types_atta = new int[maxAttachments];
        for (int i = 0; i < m_attachments.size(); ++i)
            types_atta[i] = GL_COLOR_ATTACHMENT0 + i;
        glDrawBuffers(types_atta);
    }

    public void resizeAttachments() {
        for (ColorAttachment a : m_attachments) {
            var info = a.texture.getInfo();
            info.size(m_dimensions.x, m_dimensions.y);
            a.texture.delete();
            a.texture = new Texture(info);
        }
        bindColorAttachments();

        if (m_special_attachment_id != 0) {
            switch (m_special_attachment) {
                case DEPTH_STENCIL:
                    glDeleteRenderbuffers(m_special_attachment_id);
                    break;
                case DEPTH_STENCIL_ACCESSIBLE:
                    glDeleteTextures(m_special_attachment_id);
                    break;
                case NONE:
                    break;
            }
        }
        createBindSpecialAttachment();
    }


    public void createBindSpecialAttachment() {
        switch (m_special_attachment) {
            case DEPTH_STENCIL:
                m_special_attachment_id = glGenRenderbuffers();
                glBindRenderbuffer(GL_RENDERBUFFER, m_special_attachment_id);
                if (multiSampleLevel > 1)
                    glRenderbufferStorageMultisample(GL_RENDERBUFFER, multiSampleLevel, GL_DEPTH24_STENCIL8, m_dimensions.x,
                            m_dimensions.y);
                else
                    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, m_dimensions.x, m_dimensions.y);

                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER,
                        m_special_attachment_id);
                break;
            case DEPTH_STENCIL_ACCESSIBLE:
                m_special_attachment_id = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, m_special_attachment_id);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH24_STENCIL8, m_dimensions.x, m_dimensions.y, 0,
                        GL_DEPTH_STENCIL,
                        GL_UNSIGNED_INT_24_8, 0);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);


                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, m_special_attachment_id, 0);
                break;
            case NONE:
                break;
        }
    }


    public void clearAttachments() {
        for (var attachment : m_attachments)
            attachment.texture.delete();

        if (m_special_attachment_id != 0) {
            switch (m_special_attachment) {
                case DEPTH_STENCIL:
                    glDeleteRenderbuffers(m_special_attachment_id);
                    break;
                case DEPTH_STENCIL_ACCESSIBLE:
                    glDeleteTextures(m_special_attachment_id);
                    break;
                case NONE:
                    break;
            }
        }
    }

    public FrameBuffer(Info info) {
        m_special_attachment = FBAttachment.NONE;
        m_id = 0;
        m_type = info.type;

        if (m_type == FBType.WINDOW_TARGET)
            return;

        m_id = glGenFramebuffers();
        if (m_type == FBType.NORMAL_TARGET) {
            ASSERT(info.textureInfos.length > 0, "At least one color attachment is neccessary (for some reason idk, Because I said so..)");
            m_dimensions.x = info.textureInfos[0].width;
            m_dimensions.y = info.textureInfos[0].height;

            if(m_dimensions.x == 0 || m_dimensions.y == 0){
                m_dimensions.x=16;
                m_dimensions.y=16;
                info.textureInfos[0].width=16;
                info.textureInfos[0].height=16;
            }
            //ASSERT(m_dimensions.x != 0 && m_dimensions.y != 0, "Invalid fbo dimensions");
            m_attachments = new ArrayList<>();
            for (int i = 0; i < info.textureInfos.length; i++)
                m_attachments.add(new ColorAttachment(null));

            m_special_attachment = info.specialAttachment;

            boolean noDimSpecified = false;

            for (int i = 0; i < m_attachments.size(); ++i) {
                var a = m_attachments.get(i);
                if (info.textureInfos[i].width == 0 || info.textureInfos[i].height == 0) {
                    //we have made invalid proxy
                    // a.texture = new Texture(0, info.textureInfos[i]);
                    ASSERT(false, "IDk something not good");
                    noDimSpecified = true;
                } else {
                    a.texture = new Texture(info.textureInfos[i]);
                }
            }
            if (!noDimSpecified) {
                bind();
                bindColorAttachments();
                createBindSpecialAttachment();
            }
        }
    }


    public void delete() {
        if (m_type == FBType.WINDOW_TARGET)
            return;
        glDeleteFramebuffers(m_id);
        clearAttachments();
    }

    public void createBindSpecialAttachment(FBAttachment attachment, Vector2i dim) {
        if (m_type == FBType.NORMAL_TARGET_EXTERNAL_TEXTURES) {
            if (m_special_attachment == FBAttachment.NONE) {
                m_dimensions = dim;
                m_special_attachment = attachment;
                bind();
                createBindSpecialAttachment();
            }
        }
    }

    public boolean isWindow() {
        return m_type == FBType.WINDOW_TARGET;
    }

    public void bind() {
        if (s_currently_bound == this)
            return;

        s_currently_bound = this;

        glBindFramebuffer(GL_FRAMEBUFFER, m_id);
        if (isWindow()) {
            var dim = App.get().getWindow().getDimensions();
            glViewport(0, 0, dim.x, dim.y);
        } else if (m_type == FBType.NORMAL_TARGET) {
            glViewport(0, 0, m_dimensions.x, m_dimensions.y);
        } else {
            //viewport cannot be established since size is not known
        }
    }

    public void unbind() {
        Renderer.getDefaultFBO().bind();
    }

    public void resize(int width, int height) {
        if (width == m_dimensions.x && height == m_dimensions.y)
            return;
        m_dimensions = new Vector2i(width, height);
        bind();
        resizeAttachments();
    }

    public Vector2i getSize() {
        ASSERT(m_type == FBType.NORMAL_TARGET, "");
        return m_dimensions;
    }

    public void clear(int bufferBits, Vector4f color) {
        bind();
        //ASSERT(m_is_bound, "Cannot clear fbo that is not bound");
        if ((bufferBits & GCon.BufferBit.COLOR) != 0)
            glClearColor(color.x, color.y, color.z, color.w);
        //else if (bits  BuffBit.DEPTH_BUFFER_BIT)
        //	glClearDepth(color.r);
        glClear(bufferBits);
    }
    public void clear(int bufferBits) {
       clear(bufferBits,new Vector4f(0,0,0,0));
    }

    public void attachTexture(int textureId, int attachmentNumber) {
        ASSERT(m_type != FBType.WINDOW_TARGET, "Cannot attach texture to window target");
        bind();
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + attachmentNumber, GL_TEXTURE_2D, textureId, 0);
    }

    public void attachTexture(Texture t, int attachmentNumber) {
        ASSERT(m_type != FBType.WINDOW_TARGET, "Cannot attach texture to window target");
        bind();
        glViewport(0, 0, t.width(), t.height());

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + attachmentNumber, GL_TEXTURE_2D, t.id(), 0);
    }

    public int getAttachmentID(int attachmentIndex, FBAttachment type) {
        ASSERT(m_type == FBType.NORMAL_TARGET, "");
        ASSERT(attachmentIndex < m_attachments.size(), "INVALID fbo attachment id");
        return m_attachments.get(attachmentIndex).texture.id();
    }

    public Texture getAttachment(int attachmentIndex, FBAttachment type) {
        switch (type) {
            case COLOR: {
                if (attachmentIndex >= m_attachments.size())
                    return null;
                //ASSERT(attachmentIndex < m_attachments.size(),"Invalid attachment index");
                return m_attachments.get(attachmentIndex).texture;
            }
            case DEPTH_STENCIL_ACCESSIBLE:
                //todo add depth stencil attachment texture retsrieveal

            default:
                ASSERT(false, "Invalid textureattachment request");
                return null;
        }
    }

    public Texture getAttachment(int attachmentIndex) {
     return getAttachment(attachmentIndex,FBAttachment.COLOR);
    }
    public Texture getAttachment() {
        return getAttachment(0,FBAttachment.COLOR);
    }
}
