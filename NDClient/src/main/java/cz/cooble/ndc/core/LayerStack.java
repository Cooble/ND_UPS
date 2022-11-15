package cz.cooble.ndc.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class LayerStack implements Iterable<Layer> {

    private List<Layer> m_layers = new ArrayList<>();


    public void push(Layer layer){
        m_layers.add(layer);
        layer.onAttach();
    }

    public void pop(Layer layer){
        m_layers.remove(layer);
        layer.onDetach();
    }
    public void delete(){
        for (var e:m_layers)
            e.onDetach();
        m_layers.clear();
    }


    @Override
    public Iterator<Layer> iterator() {
        return m_layers.iterator();
    }

    public ListIterator<Layer> listIterator() {
        return m_layers.listIterator(m_layers.size());
    }

}
