package cz.cooble.ndc.gui;

public class GuiContext {

    private static Object o;
    public static void setFocusedElement(Object o){
        GuiContext.o = o;
    }
    public static Object getFocusedElement(){
        return o;
    }

}
