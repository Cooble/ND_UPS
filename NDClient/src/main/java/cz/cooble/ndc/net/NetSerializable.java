package cz.cooble.ndc.net;

import cz.cooble.ndc.test.NetWriter;

public interface NetSerializable {

    void serialize(NetWriter w);
    void deserialize(NetReader b);
}
