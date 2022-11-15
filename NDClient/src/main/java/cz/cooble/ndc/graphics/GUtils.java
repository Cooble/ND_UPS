package cz.cooble.ndc.graphics;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GUtils {
    public static FloatBuffer wrapBuffer(float[] f){
        var out = BufferUtils.createFloatBuffer(f.length);
        out.put(f);
        out.flip();
        return out;
    }
    public static IntBuffer wrapBuffer(int[] f){
        var out = BufferUtils.createIntBuffer(f.length);
        out.put(f);
        out.flip();
        return out;
    }
}
