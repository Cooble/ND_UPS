package cz.cooble.ndc.core;

import cz.cooble.ndc.WorldLayer;
import cz.cooble.ndc.test.TestFBO;
import cz.cooble.ndc.graphics.GCon;
import cz.cooble.ndc.graphics.Renderer;
import cz.cooble.ndc.graphics.Window;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.WindowCloseEvent;
import cz.cooble.ndc.input.WindowResizeEvent;
import cz.cooble.ndc.world.ClientLayer;
import org.joml.Vector4f;

import static cz.cooble.ndc.Asserter.ND_TRACE;
import static org.joml.Math.max;

public class App {

    private static App s_instance;
    private boolean m_running;
    private int m_target_tps=15;
    private long m_tel_updates_per_frame;
    private int current_fps;
    private long lastFPSMillis;
    private int m_fps;

    public static App get() {
        return s_instance;
    }
    public static void main(String[] args){
        new App().start();
    }

    private LayerStack layerStack;
    private Window window;

    private Window.RealInput input;

    public App() {
        s_instance = this;
        layerStack = new LayerStack();
        window = new Window(800,800,"Hello Stanley",false);

        window.setEventCallback(this::onEventCallback);

        input = new Window.RealInput(window);

        // BASIC LAYERS
        layerStack.push(new InitLayer());

        layerStack.push(new ClientLayer());

        // CUSTOM LAYERS
      //  layerStack.push(new TestFBO());
      // layerStack.push(new TestTexture());
       // layerStack.push(new ExampleLayer());
        layerStack.push(new WorldLayer());
       // layerStack.push(new ExampleLayer2());

        m_target_tps=60;
    }

    public void render(){
        window.swapBuffers();

        Renderer.getDefaultFBO().bind();
        Renderer.getDefaultFBO().clear(GCon.BufferBit.COLOR | GCon.BufferBit.DEPTH,new Vector4f());

        for(var l:layerStack)
            l.onRender();

        window.getFBO().unbind();
    }

    public void update(){
        window.pollEvents();
        input.update();

        for(var l:layerStack)
            l.onUpdate();

    }

    public void onEventCallback(Event event) {
        if(event instanceof WindowCloseEvent){
            stop();
            event.handled=true;
            return;
        }
        var it = layerStack.listIterator() ;
        while(it.hasPrevious()){
            var l = it.previous();
            l.onEvent(event);

            if(event instanceof WindowResizeEvent)
                l.onWindowResize(((WindowResizeEvent) event).getWidth(),((WindowResizeEvent) event).getHeight());

            if(event.handled)
                break;
        }
    }

    public Window.RealInput getInput() {
        return input;
    }

    public Window getWindow() {
        return window;
    }

    private long nowTime(){
        return System.currentTimeMillis();
    }
    
    public void start() {
        ND_TRACE("Game started");

        m_running = true;
        var lastTime = nowTime();
        int millisPerTick = 1000 / m_target_tps;
        long accomodator = 0;
        var lastRenderTime = nowTime();
        while (m_running)
        {
            var now = nowTime();
            accomodator += now - lastTime;
            lastTime = now;
            m_tel_updates_per_frame = accomodator / millisPerTick;
            if (m_tel_updates_per_frame > 20)
                accomodator = 0; //lets slow the game to catch up
            int maxMil = 0;
            while (accomodator >= millisPerTick)
            {
                accomodator -= millisPerTick;

                var tt = nowTime();
                update();
                maxMil = max((int)(nowTime() - tt), maxMil);
            }

            if (nowTime() - lastRenderTime < 1000 / m_target_tps)
                //if (nowTime() - lastRenderTime < 1000 / 50)
                continue; //skip render
            now = nowTime();
            lastRenderTime = now;
            if (!window.isIconified())
                render();

            current_fps++;
            var noww = nowTime();
            if (noww - lastFPSMillis > 500)
            {
                m_fps = current_fps * 2;
                current_fps = 0;
                lastFPSMillis = nowTime();
            }

            fpsPrinter();
        }

        layerStack.delete();
        window.close();
        //ND_TRACE("Saving settings.json");
        //NBT::saveToFile("settings.json", *m_settings);
        ND_TRACE("Game quitted");
    }


    private long lastTimeFpsPrint;
    private static final long FPS_PRINT_INTERVAL =1000;
    private void fpsPrinter(){
        var now = nowTime();
        if(now-lastTimeFpsPrint>FPS_PRINT_INTERVAL){
            lastTimeFpsPrint = now;
            //ND_TRACE("FPS: "+m_fps);
        }
    }
    public void stop(){
        m_running=false;
    }

    public long getLastFPSMillis() {
        return lastFPSMillis;
    }
}
