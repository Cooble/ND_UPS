package cz.cooble.ndc.physics;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.joml.Math.max;
import static org.joml.Math.min;

public class Polygon {
    List<Vector2f> list;
    public boolean isRectangle = false;
    Rect bounds = new Rect(0,0,0,0);

    public Polygon(Vector2f... v) {
        list = Arrays.asList(v);
        recalculateBounds();
    }

    public Polygon(Polygon p) {
        list=new ArrayList<>();
        for(var a:p.list)
            list.add(new Vector2f(a));

        isRectangle = p.isRectangle;
        bounds = new Rect(p.bounds);
    }

    public void recalculateBounds() {
        bounds.x0 = Float.MAX_VALUE;
        bounds.y0 = Float.MAX_VALUE;

        bounds.x1 = -Float.MAX_VALUE;
        bounds.y1 = -Float.MAX_VALUE;

        for (var v : list) {
            bounds.x0 = min(bounds.x0, v.x);
            bounds.x1 = max(bounds.x1, v.x);
            bounds.y0 = min(bounds.y0, v.y);
            bounds.y1 = max(bounds.y1, v.y);
        }
    }

    public Vector2f getVec(int index) {
        return new Vector2f(list.get(index));
    }

    public Polygon plus(Vector2f v) {
        for (var vec : list)
            vec.add(v);
        recalculateBounds();
        return this;
    }

    public Polygon minus(Vector2f v) {
        for (var vec : list)
            vec.sub(v);
        recalculateBounds();
        return this;
    }

    public int size() {
        return list.size();
    }

    public Rect getBounds() {
        return bounds;
    }

    public Polygon copy() {
        return new Polygon(this);
    }
    public static Polygon toPolygon(Rect r)
    {
        var out = new Polygon(
                new Vector2f(r.x0, r.y0),
               new Vector2f(r.x1, r.y0),
               new Vector2f(r.x1, r.y1),
               new Vector2f(r.x0, r.y1)
        );
        out.isRectangle = true;
        return out;
    }
    // lines collisions ================================================================

    // returns the point of intersection between line a and b or if (no point or every point) -> invalid
    public static  Vector2f intersectLines( Vector2f pa0,  Vector2f pa1,  Vector2f pb0,  Vector2f pb1)
    {
        var deltaA = new Vector2f(pa1).sub(pa0);
        var deltaB = new Vector2f(pb1).sub(pb0);

        float t = (deltaB.x * (pb0.y - pa0.y) + deltaB.y * (pa0.x - pb0.x)) / (deltaB.x * deltaA.y - deltaA.x * deltaB.y);
        return new Vector2f(pa0).add(new Vector2f(deltaA).mul(t));
    }

    public static  boolean isBetween(float val, float bound0, float bound1)
    {
      //  return val >= bound0 && val <= bound1; //without branch prediction it should be faster...
        return ((bound0 - val) * (val - bound1)) >= 0;
    }


    public static boolean isValidFloat(float f)
    {
        return !Float.isNaN(f) && !Float.isInfinite(f);
    }
    public static boolean isValid(Vector2f v) { return isValidFloat(v.x) && isValidFloat(v.y); }

    // returns the point of intersection between abscisses a and b or if (no point or every point) -> invalid
    public static  Vector2f intersectAbscisses( Vector2f pa0,  Vector2f pa1,  Vector2f pb0,  Vector2f pb1)
    {
        Vector2f v = intersectLines(pa0, pa1, pb0, pb1);
        if (!isValid(v))
            return v;

        if (
                isBetween(v.x, pa0.x, pa1.x) &&
                        isBetween(v.x, pb0.x, pb1.x) &&
                        isBetween(v.y, pa0.y, pa1.y) &&
                        isBetween(v.y, pb0.y, pb1.y))
            return v;
        return invalidVec2();
    }
    public static Vector2f invalidVec2() {
        return new Vector2f(Float.NaN, Float.NaN);
    }
    
    // checks if abscisses a and b intersect at one point
    public static  boolean isIntersectAbscisses( Vector2f pa0,  Vector2f pa1,  Vector2f pb0,  Vector2f pb1)
    {
        var v = intersectLines(pa0, pa1, pb0, pb1);
        if (!isValid(v))
            return false;

        return
                isBetween(v.x, pa0.x, pa1.x) &&
                        isBetween(v.x, pb0.x, pb1.x) &&
                        isBetween(v.y, pa0.y, pa1.y) &&
                        isBetween(v.y, pb0.y, pb1.y);
    }

    //polygons =========================================================================

    public static  boolean isIntersects( Polygon a,  Polygon b)
    {
        if (!a.getBounds().intersects(b.getBounds())) //first check if it is even worth trying
            return false;

        
        for (int i = 0; i < a.size() - 1; ++i)
        //it is that big because not using modulo for p1,p2 (bigger code - bigger performance)
        {
            var p0 = a.getVec(i);
            var p1 = a.getVec(i + 1);

            for (int j = 0; j < b.size() - 1; ++j)
            {
                var p2 = b.getVec(j);
                var p3 = b.getVec(j + 1);
                if (isIntersectAbscisses(p0, p1, p2, p3))
                    return true;
            }
            var p2 = b.getVec(b.size() - 1);
            var p3 = b.getVec(0);
            if (isIntersectAbscisses(p0, p1, p2, p3))
                return true;
        }

        var p0 = b.getVec(a.size() - 1);
        var p1 = b.getVec(0);

        for (int j = 0; j < b.size() - 1; ++j)
        {
            var p2 = b.getVec(j);
            var p3 = b.getVec(j + 1);
            if (isIntersectAbscisses(p0, p1, p2, p3))
                return true;
        }
        var p2 = b.getVec(b.size() - 1);
        var p3 = b.getVec(0);
        return isIntersectAbscisses(p0, p1, p2, p3);
    }

    public static  boolean isIntersects( Polygon aa,  Vector2f aPos,  Polygon b,  Vector2f bPos)
    {
        Polygon a = aa.copy().plus(new Vector2f(aPos).sub(bPos));
        return isIntersects(a, b);
    }

    public static  boolean contains( Polygon a,  Vector2f point)
    {
        if (!a.getBounds().containsPoint(point))
            return false;
        if (a.isRectangle)
            return true;
        Vector2f secPoint = new Vector2f(0, 123451526789.f);
        int intersections = 0;
        for (int i = 0; i < a.size(); ++i)
        {
            var p0 = a.getVec(i);
            var p1 = a.getVec((i + 1) % a.size());


            if (isIntersectAbscisses(p0, p1, point, secPoint))
                ++intersections;
        }
        return (intersections & 1)!=0;
    }

    public static  Vector2f intersects( Polygon a,  Polygon b)
    {
        if (a.getBounds().intersects(b.getBounds())) //first check if it is even worth trying
        {
            for (int i = 0; i < a.size() - 1; ++i)
            //it is that big because not using modulo for p1,p2 (bigger code - bigger performance)
            {
                var p0 = a.getVec(i);
                var p1 = a.getVec(i + 1);

                for (int j = 0; j < b.size() - 1; ++j)
                {
                    var p2 = b.getVec(j);
                    var p3 = b.getVec(j + 1);
                    var out = intersectAbscisses(p0, p1, p2, p3);
                    if (isValid(out))
                        return out;
                }
                var p2 = b.getVec(b.size() - 1);
                var p3 = b.getVec(0);
                var out = intersectAbscisses(p0, p1, p2, p3);
                if (isValid(out))
                    return out;
            }

            var p0 = a.getVec(a.size() - 1);
            var p1 = a.getVec(0);

            for (int j = 0; j < b.size() - 1; ++j)
            {
                var p2 = b.getVec(j);
                var p3 = b.getVec(j + 1);
                var out = intersectAbscisses(p0, p1, p2, p3);
                if (isValid(out))
                    return out;
            }
            var p2 = b.getVec(b.size() - 1);
            var p3 = b.getVec(0);
            var out = intersectAbscisses(p0, p1, p2, p3);
            if (isValid(out))
                return out;
        }
        return invalidVec2();
    }
}
