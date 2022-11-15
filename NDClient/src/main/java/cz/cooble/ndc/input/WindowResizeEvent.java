package cz.cooble.ndc.input;

public class WindowResizeEvent extends WindowEvent{

    private int w,h;
    public WindowResizeEvent(int w,int h) {
        super(EventType.WindowResize);
        this.w=w;
        this.h=h;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }
}
