package cz.cooble.ndc.core;

import cz.cooble.ndc.graphics.Effect;

public class InitLayer extends Layer {


    @Override
    public void onAttach() {
        Effect.init();
    }
}
