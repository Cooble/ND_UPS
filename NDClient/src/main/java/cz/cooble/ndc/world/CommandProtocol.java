package cz.cooble.ndc.world;

import cz.cooble.ndc.net.NetBuffer;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.net.SessionProtocol;

public class CommandProtocol extends SessionProtocol {

    public String message = "";

    public CommandProtocol(){
        this.action= Prot.Command;
    }
    public CommandProtocol(String message){this.message=message;}

    @Override
    public void serialize(NetBuffer b) {
        super.serialize(b);
        b.put(message);
    }

    @Override
    public void deserialize(NetBuffer b) {
        super.deserialize(b);
        message = b.getString();
    }
}
