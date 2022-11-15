package cz.cooble.ndc.input;

public class KeyPressEvent extends KeyEvent{


    private boolean isRelease;
    private int keycode;
    private boolean isRepeating;

    public KeyPressEvent(int key,int mods,boolean isRelease,boolean isRepeating) {
        super(EventType.KeyPress, mods);
        this.isRelease=  isRelease;
        this.keycode = key;
        this.isRepeating = isRepeating;
    }
    public boolean isRelease() {
        return isRelease;
    }

    public boolean isRepeating() {
        return isRepeating;
    }

    public int getKeycode() {
        return keycode;
    }

    public int  getFreshKeycode(){
        return isRepeating?-1:keycode;
    }
}
