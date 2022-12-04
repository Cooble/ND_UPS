package cz.cooble.ndc.input;

public class KeyTypeEvent extends KeyEvent{
    private int keycode;

    public KeyTypeEvent(int keycode, int mods) {
        super(EventType.KeyPress, mods);
        this.keycode = keycode;

    }
    public int getKeycode() {
        return keycode;
    }

}
