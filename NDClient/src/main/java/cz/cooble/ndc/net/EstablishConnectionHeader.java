package cz.cooble.ndc.net;

public class EstablishConnectionHeader extends SessionProtocol {
    public String player_name = "";
    public String server_name = "";

    public void serialize(NetBuffer b) {
        super.serialize(b);
        b.putString(player_name);
        b.putString(server_name);
    }

    public void deserialize(NetBuffer b) {
        super.deserialize(b);
        player_name = b.getString();
        server_name = b.getString();
    }
}
