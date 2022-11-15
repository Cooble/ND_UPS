package cz.cooble.ndc.input;

public class KeyTypeEvent extends KeyEvent{
    private int keycode;

    public KeyTypeEvent(int mods, int key) {
        super(EventType.KeyPress, mods);
        this.keycode = key;

    }
    public int getKeycode() {
        return keycode;
    }
}
