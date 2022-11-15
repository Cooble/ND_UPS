package cz.cooble.ndc.core;

import cz.cooble.ndc.input.Event;

public class Layer {

    private String m_name;

    public Layer(String name) {
        this.m_name = name;
    }
    public Layer() {
        this.m_name = getClass().getName();
    }

    public void onAttach() {
    }

    public void onDetach() {
    }

    public void onUpdate() {
    }

    public void onRender() {
    }

    public void onWindowResize(int width, int height) {
    }

    public void onImGuiRender() {
    }

    public void onEvent(Event e) {
    }

    public String getName() {
        return m_name;
    }

}

