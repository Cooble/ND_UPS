package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;

public class ControlProtocol extends CommandProtocol {
    public ControlProtocol(){
        this.action = Prot.Quit;
    }

    public ControlProtocol(String message){
        this.message=message;
        this.action= Prot.Quit;
    }
}
