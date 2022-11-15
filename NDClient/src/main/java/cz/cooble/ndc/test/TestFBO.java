package cz.cooble.ndc.test;

import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.graphics.*;

import static cz.cooble.ndc.graphics.GUtils.wrapBuffer;


public class TestFBO extends Layer {

    private Texture texture;
    private FrameBuffer frameBuffer;

    @Override
    public void onAttach() {
        texture = new Texture(new Texture.Info("res/images/background.png"));
        frameBuffer = new FrameBuffer(new FrameBuffer.Info(new Texture.Info().size(800,800)));
    }

    @Override
    public void onRender() {
        Effect.render(texture,frameBuffer);
        Effect.render(frameBuffer.getAttachment(),Renderer.getDefaultFBO());
    }
}
