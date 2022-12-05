package cz.cooble.ndc.world;

import cz.cooble.ndc.net.NetBuffer;
import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.net.SessionProtocol;
import cz.cooble.ndc.test.NetWriter;

public class CommandProtocol extends SessionProtocol {

    public String message = "";

    public CommandProtocol(){
        this.action= Prot.Command;
    }
    public CommandProtocol(String message){this.message=message;}

    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.put(message);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        message = b.getString();
    }
}
