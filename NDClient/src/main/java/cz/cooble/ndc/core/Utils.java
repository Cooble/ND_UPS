package cz.cooble.ndc.core;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.system.MemoryUtil.memSlice;

public class Utils {
    public static int half_int(int x, int y) {
        return ((y << 16) & 0xffff0000) | (x & 0x0000ffff);
    }
    public static Vector2i half_int_object(int i){
        return new Vector2i(getX(i),getY(i));
    }
    public static int getX(int i){
        return i & 0x0000ffff;
    }
    public static int getY(int i){
        return (i>>16) & 0x0000ffff;
    }
    public static int plus_half_int(int a, int b){
        return half_int(getX(a)+getX(b),getY(a)+getY(b));
    }
    public static int BIT(int i){return 1<<i;}

    public static boolean BOOL(int i){
        return i!=0;
    }
    public static int INT(boolean i){
        return i?1:0;
    }

    public static float clamp(float v,float min,float max){
         return Math.min(Math.max(min,v),max);
    }
    public static Vector2f clamp(Vector2f v,Vector2f min,Vector2f max){
        return new Vector2f(clamp(v.x,min.x,max.x),clamp(v.y,min.y,max.y));
    }
}
