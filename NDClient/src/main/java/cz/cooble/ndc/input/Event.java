package cz.cooble.ndc.input;

public class Event {
    private final EventType type;

    public enum KeyMods
    {
        Shift(1<<0),
        Control(1<<1),
        Alt(1<<2),
        Super(1<<3),
        CapsLock(1<<4),
        NumLock(1<<5);

        public final int i;
        KeyMods(int i) {
            this.i = i;
        }
    };

    public enum EventType
    {
        WindowClose,
        WindowResize,
        MousePress,
        MouseScroll,
        MouseMove,
        MouseFocusGain,
        MouseFocusLost,
        MouseDrag,
        MouseEnter,
        KeyPress,
        KeyType,
        Message,
        Drop
    };

    public boolean handled = false;
    private int m_mods = 0;

    public Event(EventType type){
        this.type = type;
    }
    public Event(EventType type,int mods){
        this.type = type;
        this.m_mods = mods;
    }

    public void markHandled(){
        this.handled=true;
    }

    public boolean isHandled() {
        return handled;
    }

    public EventType getType() {
        return type;
    }
    public boolean isAltPressed() { return (m_mods & KeyMods.Alt.i)!=0; }
    public boolean isControlPressed() { return (m_mods & KeyMods.Control.i)!=0; }
    public boolean isShiftPressed() { return (m_mods & KeyMods.Shift.i)!=0; }
}
