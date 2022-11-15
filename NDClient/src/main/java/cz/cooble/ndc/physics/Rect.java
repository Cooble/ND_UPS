package cz.cooble.ndc.physics;

import org.joml.Vector2f;

import java.util.List;


public class Rect {
    public float x0, y0, x1, y1;

    public Rect(float x0, float y0, float x1, float y1) {
        this.x0 = x0;
        this.x1 = x1;
        this.y0 = y0;
        this.y1 = y1;
    }

    public Rect(Rect bounds) {
        this.x0 = bounds.x0;
        this.x1 = bounds.x1;
        this.y0 = bounds.y0;
        this.y1 = bounds.y1;
    }


    public boolean containsPoint(float x, float y) {
        return (x >= x0) && (x <= x1) && (y >= y0) && (y <= y1);
    }

    public boolean containsPoint(Vector2f v) {
        return (v.x >= x0) && (v.x <= x1) && (v.y >= y0) && (v.y <= y1);
    }

    public float width() {
        return x1 - x0;
    }

    public float height() {
        return y1 - y0;
    }

    public void add(int x, int y) {
        x0 += x;
        x1 += x;
        y0 += y;
        y1 += y;
    }

    public void add(Vector2f v) {
        add(v.x, v.y);
    }

    public void add(float x, float y) {
        x0 += x;
        x1 += x;
        y0 += y;
        y1 += y;
    }

    public void minus(int x, int y) {
        x0 -= x;
        x1 -= x;
        y0 -= y;
        y1 -= y;
    }

    public void minus(Vector2f v) {
        minus(v.x, v.y);
    }

    public void minus(float x, float y) {
        x0 -= x;
        x1 -= x;
        y0 -= y;
        y1 -= y;
    }

    public static Rect createFromDimensions(float x, float y, float width, float height) {
        return new Rect(x, y, x + width, y + height);
    }

    public void setWidth(float width) {
        x1 = x0 + width;
    }

    public void setHeight(float height) {
        y1 = y0 + height;
    }


    private boolean intersectsInner(Rect rect) {
        return
                rect.containsPoint(x0, y0) ||
                        rect.containsPoint(x1, y0) ||
                        rect.containsPoint(x0, y1) ||
                        rect.containsPoint(x1, y1);
    }


    public boolean intersects(Rect rect) {
        return intersectsInner(rect) || rect.intersectsInner(this);
    }
}
