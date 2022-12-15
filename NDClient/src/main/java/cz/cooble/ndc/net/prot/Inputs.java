package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.NetSerializable;
import cz.cooble.ndc.test.NetWriter;

public class Inputs implements NetSerializable {
    private static int bti(boolean b) {return b ? 1 : 0;}



    public boolean up, down, left, right;

    @Override
    public void serialize(NetWriter w) {
        w.put(toString());
    }

    @Override
    public void deserialize(NetReader b) {
        var s = b.getString();
        up = s.charAt(0)=='1';
        down = s.charAt(1)=='1';
        left = s.charAt(2)=='1';
        right = s.charAt(3)=='1';
    }

    @Override
    public String toString() {
        return "" + bti(up) + bti(down) + bti(left) + bti(right);
    }
}

