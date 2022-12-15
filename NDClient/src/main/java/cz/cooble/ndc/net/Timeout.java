package cz.cooble.ndc.net;

import java.util.HashMap;
import java.util.Map;

import static cz.cooble.ndc.core.App.nowTime;

public class Timeout {

    public static class Pocket {
        long timeout,time;
        boolean enabled;

        public Pocket(long timeout){this.timeout=timeout;}

        public void start(){
            enabled=true;
            time = nowTime();
        }
        public void startWithTimeout(){
            enabled = true;
            time = 0;
        }
        public void stop(){
            enabled=false;
        }
        public boolean isTimeout(){
            return nowTime()-time>timeout && enabled;
        }

        public boolean isRunning() {
            return enabled;
        }
    }


    private Map<String, Pocket> t = new HashMap<>();


    public void start(String id){
        var e = t.get(id);
        e.start();
    }
    public void startWithTimeout(String id){
        var e = t.get(id);
        e.startWithTimeout();

    }
    public void stop(String id){
        var e = t.get(id);
        e.stop();
    }

    public void register(String id,long timeout){
        var e = new Pocket(timeout);
        t.put(id,e);
    }

    public boolean isTimeout(String id){
        var e = t.get(id);
        return e.isTimeout();
    }
}
