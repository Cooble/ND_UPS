package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Positions of players
 *
 * From server to client
 */
public class PlayersMoved extends ProtocolHeader{
    public List<PlayerMoved> moves = new ArrayList<>();

    public PlayersMoved(){
        action = Prot.PlayersMoved;
    }

    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.putArray(moves);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        moves = b.getArray(PlayerMoved::new);
    }
}
