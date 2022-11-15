package cz.cooble.ndc.graphics;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

public class GCon {
    public class BufferBit {
        public static final int COLOR = GL_COLOR_BUFFER_BIT;
        public static final int DEPTH = GL_DEPTH_BUFFER_BIT;
        public static final int STENCIL = GL_STENCIL_BUFFER_BIT;
    }
    public static int toGL(g_typ type)
    {
        switch (type)
        {
            case UNSIGNED_INT: return GL_UNSIGNED_INT;
            case FLOAT: return GL_FLOAT;
            case INT: return GL_INT;
            case VEC2: return GL_FLOAT_VEC2;
            case VEC3: return GL_FLOAT_VEC3;
            case VEC4: return GL_FLOAT_VEC4;
            case MAT3: return GL_FLOAT_MAT3;
            case MAT4: return GL_FLOAT_MAT4;

            case IVEC2: return GL_INT_VEC2;
            case IVEC3: return GL_INT_VEC3;
            case IVEC4: return GL_INT_VEC4;
            case TEXTURE_2D: return GL_TEXTURE_2D;
            case TEXTURE_CUBE: return GL_TEXTURE_CUBE_MAP;
            case BYTE: return GL_BYTE;
            case UNSIGNED_BYTE: return GL_UNSIGNED_BYTE;
            case SHORT: return GL_SHORT;
            case UNSIGNED_SHORT: return GL_UNSIGNED_SHORT;
            default: return GL_INVALID_ENUM;
        }
    }

    public enum Blend {
        ZERO(GL_ZERO),
        ONE(GL_ONE),
        SRC_COLOR(GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GL_ONE_MINUS_SRC_COLOR),
        SRC_ALPHA(GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GL_ONE_MINUS_DST_ALPHA),
        DST_COLOR(GL_DST_COLOR),
        ONE_MINUS_DST_COLOR(GL_ONE_MINUS_DST_COLOR),
        CONSTANT_COLOR(GL_CONSTANT_COLOR),
        ONE_MINUS_CONSTANT_COLOR(GL_ONE_MINUS_CONSTANT_COLOR),
        CONSTANT_ALPHA(GL_CONSTANT_ALPHA),
        ONE_MINUS_CONSTANT_ALPHA(GL_ONE_MINUS_CONSTANT_ALPHA),
        NONE(-1);
        public final int i;

        Blend(int i) {
            this.i = i;
        }
    }


    public enum BlendEquation {
        FUNC_ADD(GL_FUNC_ADD),
        FUNC_REVERSE_SUBTRACT(GL_FUNC_REVERSE_SUBTRACT),
        FUNC_SUBTRACT(GL_FUNC_SUBTRACT),
        MIN(GL_MIN),
        MAX(GL_MAX),
        NONE(-1);

        public final int i;

        BlendEquation(int i) {
            this.i = i;
        }
    }

    public enum StencilFunc {
        NEVER(GL_NEVER),
        LESS(GL_LESS),
        LEQUAL(GL_LEQUAL),
        GREATER(GL_GREATER),
        GEQUAL(GL_GEQUAL),
        EQUAL(GL_EQUAL),
        NOTEQUAL(GL_NOTEQUAL),
        ALWAYS(GL_ALWAYS);

        public final int i;

        StencilFunc(int i) {
            this.i = i;
        }
    }

    public enum StencilOp {
        KEEP(GL_KEEP),
        ZERO(GL_ZERO),
        REPLACE(GL_REPLACE),
        INCR(GL_INCR),
        INCR_WRAP(GL_INCR_WRAP),
        DECR(GL_DECR),
        DECR_WRAP(GL_DECR_WRAP);

        public final int i;

        StencilOp(int i) {
            this.i = i;
        }
    }


    public enum Topology {
        LINES(GL_LINES),
        TRIANGLES(GL_TRIANGLES),
        TRIANGLE_FAN(GL_TRIANGLE_FAN),
        TRIANGLES_ADJACENCY(GL_TRIANGLES_ADJACENCY),
        TRIANGLE_STRIP(GL_TRIANGLE_STRIP);

        public final int i;

        Topology(int i) {
            this.i = i;
        }
    }

    public enum RenderStage {
        VERTEX(1),
        FRAGMENT(2),
        GEOMETRY(4);

        public final int i;

        RenderStage(int i) {
            this.i = i;
        }
    }

    public enum VertexType {
        POS,
        NORMAL,
        UV,
        INVALID
    }

    public static class GTypes {
        public static boolean isIType(g_typ type) {
            switch (type) {
                case UNSIGNED_INT:
                case INT:
                case IVEC2:
                case IVEC3:
                case IVEC4:
                case UNSIGNED_BYTE:
                case SHORT:
                case UNSIGNED_SHORT:
                    return true;

                default:
                    return false;
            }
        }

        public static boolean isTexture(g_typ type) {
            return type == g_typ.TEXTURE_2D || type == g_typ.TEXTURE_CUBE;
        }

        public static int getCount(g_typ type) {
            switch (type) {
                case BYTE:
                case UNSIGNED_BYTE:
                case SHORT:
                case UNSIGNED_SHORT:
                case UNSIGNED_INT:
                case FLOAT:
                case INT:
                case MAT3:
                case MAT4:
                case TEXTURE_2D:
                case TEXTURE_CUBE:
                    return 1;
                case IVEC2:
                case VEC2:
                    return 2;
                case VEC3:
                case IVEC3:
                    return 3;
                case VEC4:
                case IVEC4:
                    return 4;
                default:
                    return 0;
            }
        }

        // base type of vector e.g. (vec2 is float, ivec3 is int)
        // otherwise return argument
        public static g_typ getBase(g_typ type) {
            switch (type) {
                case VEC2:
                case VEC3:
                case VEC4:
                    return g_typ.FLOAT;
                case IVEC2:
                case IVEC3:
                case IVEC4:
                    return g_typ.INT;
                default:
                    return type;
            }
        }

        g_typ getType(String view) {
            if (view.equals("float"))
                return g_typ.FLOAT;
            if (view.equals("vec2"))
                return g_typ.VEC2;
            if (view.equals("vec3"))
                return g_typ.VEC3;
            if (view.equals("vec4"))
                return g_typ.VEC4;
            if (view.equals("mat3"))
                return g_typ.MAT3;
            if (view.equals("mat4"))
                return g_typ.MAT4;

            if (view.equals("int"))
                return g_typ.INT;
            if (view.equals("ivec2"))
                return g_typ.IVEC2;
            if (view.equals("ivec3"))
                return g_typ.IVEC3;
            if (view.equals("ivec4"))
                return g_typ.IVEC4;
            if (view.equals("uint"))
                return g_typ.UNSIGNED_INT;
            if (view.equals("sampler2D"))
                return g_typ.TEXTURE_2D;
            if (view.equals("samplerCube"))
                return g_typ.TEXTURE_CUBE;
            return g_typ.INVALID;
        }

        public static String getName(g_typ type) {
            switch (type) {
                case UNSIGNED_INT: return "uint";
                case FLOAT: return "float";
                case VEC2: return "vec2";
                case VEC3: return "vec3";
                case VEC4: return "vec4";
                case INT: return "int";
                case IVEC2: return "ivec2";
                case IVEC3: return "ivec3";
                case IVEC4: return "ivec4";
                case MAT3: return "mat3";
                case MAT4: return "mat4";
                case TEXTURE_2D: return "sampler2D";
                case TEXTURE_CUBE: return "samplerCube";
                default:
                    return "INVALID TYPE";
            }
        }

        public static int GTYPE_SIZES[] = {
                1, //	BYTE,
                1, //UNSIGNED_BYTE,
                2, //SHORT,
                2, //UNSIGNED_SHORT,
                4, //UNSIGNED_INT,
                4, //FLOAT,
                4 * 2, //VEC2,
                4 * 3, //VEC3,
                4 * 4, //VEC4,
                4, //INT,
                4 * 2, //IVEC2,
                4 * 3, //IVEC3,
                4 * 4, //IVEC4,
                4 * 3 * 3, //MAT3,
                4 * 4 * 4, //MAT4,
                4, //TEXTURE,
                4, //TEXTURE_CUBE,
                0 //INVALID
        };

        public static int getSize(g_typ type) {
            return GTYPE_SIZES[type.ordinal()];
        }

        public static g_typ setCount(g_typ base, int count) {
            switch (base) {
                case FLOAT:
                case INT:
                    return g_typ.values()[base.ordinal() + count - 1];
                default:
                    //ASSERT(false, "invalid base type");
                    return g_typ.INVALID;
            }
        }
    }

    public static void enableBlend() {
        glEnable(GL_BLEND);
    }

    public static void disableBlend() {
        glDisable(GL_BLEND);
    }

    public static void blendEquation(BlendEquation e) {
        glBlendEquation(e.i);
    }

    public static void blendFuncSeparate(Blend src, Blend dst, Blend srcA, Blend dstA) {
        glBlendFuncSeparate(src.i,dst.i, srcA.i, dstA.i);
    }

    public static void blendFunc(Blend src, Blend dst)
    {
        glBlendFunc(src.i, dst.i);
    }

    public static void blendColor(float r, float g, float b, float a) {
        glBlendColor(r, g, b, a);
    }

    public static void enableDepthTest(boolean enable) {
        if (enable)
            glEnable(GL_DEPTH_TEST);
        else
            glDisable(GL_DEPTH_TEST);
    }

    public static void enableCullFace(boolean enable) {
        if (enable)
            glEnable(GL_CULL_FACE);
        else
            glDisable(GL_CULL_FACE);
    }

    public static void depthMask(boolean val) {
        glDepthMask(val);
    }

    public static void enableStencilTest(boolean enable) {
        if (enable)
            glEnable(GL_STENCIL_TEST);
        else
            glDisable(GL_STENCIL_TEST);
    }

    public static void clear(int bits) {
        glClear(bits);
    }

    public static void stencilOp(StencilOp stfails, StencilOp dtfails, StencilOp dtpass) {
        glStencilOp(stfails.i,dtfails.i, dtpass.i);
    }

    public static void stencilMask(int mask) {
        glStencilMask(mask);
    }

    public static void stencilFunc(StencilFunc func, int value, int mask) {
        glStencilFunc(func.i, value, mask);
    }

    public static void clearColor(float r, float g, float b, float a) {
        glClearColor(r, g, b, a);
    }

    public static void viewport(int x, int y, int width, int height) {
        glViewport(x, y, width, height);
    }

    public static void cmdDrawElements(Topology t, int elementLength) {
        glDrawElements(t.i, elementLength, GL_UNSIGNED_INT, 0);
    }

    public static void cmdDrawMultiElements(Topology t, int[] startIndexes, int[] lengths,int count) {
        // f*ck Java
        for (int i = 0; i < count; i++)
            glDrawElements(t.i, lengths[i], GL_UNSIGNED_INT, startIndexes[i]);
    }

    public static void cmdDrawArrays(Topology t, int offset, int elementLength) {
        glDrawArrays(t.i, offset, elementLength);
    }
}

