package cz.cooble.ndc.net;

public interface NetSerializable {

    void serialize(NetBuffer b);
    void deserialize(NetBuffer b);
}
