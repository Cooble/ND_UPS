package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;

/**
 * Sent in tcp tunnel
 * Informs about changes of players
 * i.e. fly mode activated for player
 * connect, disconnected player
 */
public class PlayerState extends ProtocolHeader {
    public String name;

    public enum Type {
        Fly,
        Connect
    }
    public Type type;
    public boolean value;


    public PlayerState() {action = Prot.PlayerState;}

    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.put(name);
        b.put(type.ordinal());
        b.put(value);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        name = b.getString();
        type = Type.values()[b.getInt()];
        value = b.getBool();
    }
}
