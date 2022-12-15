package cz.cooble.ndc.net.prot;


import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.NetSerializable;
import cz.cooble.ndc.test.NetWriter;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Server sends to client
 */
public class PlayerMoved implements NetSerializable {

    // player
    public String name;
    // inputs issued by the player in order to get to targetPos
    public List<Inputs> inputs = new ArrayList<>();
    // final position that the player got to
    public Vector2f targetPos = new Vector2f();
    public Vector2f targetVelocity = new Vector2f();
    // latest event move id from client
    public int event_id;

    @Override
    public void serialize(NetWriter b) {
        b.put(name);
        b.putArray(inputs);
        b.put(targetPos);
        b.put(targetVelocity);
        b.put(event_id);
    }

    @Override
    public void deserialize(NetReader b) {
        name = b.getString();
        inputs = b.getArray(Inputs::new);
        targetPos = b.getVector2f();
        targetVelocity = b.getVector2f();
        event_id = b.getInt();
    }
}
