package cz.cooble.ndc.net;

import cz.cooble.ndc.test.NetWriter;

public class EstablishConnectionHeader extends SessionProtocol {
    public String player_name = "";
    public String server_name = "";

    public void serialize(NetWriter b) {
        super.serialize(b);
        b.putString(player_name);
        b.putString(server_name);
    }

    public void deserialize(NetReader b) {
        super.deserialize(b);
        player_name = b.getString();
        server_name = b.getString();
    }
}
