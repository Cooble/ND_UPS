package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;
import org.joml.Vector2f;

/**
 * Inputs from client to server
 */
public class PlayerMoves extends SessionProtocol{

    public Inputs inputs = new Inputs();
    public Vector2f pos=new Vector2f();
    public int event_id;

    public PlayerMoves(){action= Prot.PlayerMoves;}

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        inputs = b.get(Inputs::new);
        pos = b.getVector2f();
        event_id = b.getInt();
    }

    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.put(inputs);
        b.put(pos);
        b.put(event_id);
    }
}
