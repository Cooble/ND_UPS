package cz.cooble.ndc;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.core.Pair;
import cz.cooble.ndc.graphics.*;
import cz.cooble.ndc.gui.GuiScreen;
import cz.cooble.ndc.gui.ScreenConnect;
import cz.cooble.ndc.gui.ScreenInfo;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.KeyPressEvent;
import cz.cooble.ndc.input.MouseMoveEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class GuiLayer extends Layer {

    private Texture texture;
    BatchRenderer2D renderer;

    ScreenConnect screenConnect;
    ScreenInfo screenInfo;
    GuiScreen currentScreen;

    boolean enabled = true;


    @Override
    public void onAttach() {
        super.onAttach();
        texture = new Texture(new Texture.Info().path("res/images/bg.png").wrapMode(Texture.WrapMode.REPEAT));
        renderer = new BatchRenderer2D();

        screenConnect = new ScreenConnect();
        screenConnect.btnConnect.onClick = () -> {
            System.out.println("clicked");
        };
        screenConnect.txtIp.setText("127.0.0.1:"+Globals.LOBBY_PORT);
        screenInfo = new ScreenInfo();
        currentScreen = screenConnect;
    }

    public void openConnectScreen(Consumer<Pair<String, String>> onIpEntered) {
        setEnabled(true);
        currentScreen = screenConnect;
        screenConnect.btnConnect.onClick = () ->
                onIpEntered.accept(new Pair<>(screenConnect.txtPlayer.getText(), screenConnect.txtIp.getText()));
    }

    public void openInfoScreen(String info, @Nullable Runnable onClicked) {
        setEnabled(true);
        currentScreen = screenInfo;
        screenInfo.setInfo(info);

        screenInfo.btnOk.enabled = onClicked != null;
        screenInfo.btnOk.onClick = onClicked;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onRender() {
        if (!enabled)
            return;

        Effect.getDefaultShader().bind();
        Effect.getDefaultShader().setUniformMat4("u_transform_uv", new Matrix4f()
                .scale(new Vector3f(App.get().getWindow().getWidth() / 64, App.get().getWindow().getHeight() / 64, 1)));
        Effect.render(texture, Renderer.getDefaultFBO());
        Effect.getDefaultShader().setUniformMat4("u_transform_uv", new Matrix4f());

        renderer.begin(Renderer.getDefaultFBO());
        renderer.push(new Matrix4f().translate(new Vector3f(-1, -1, 0)));
        renderer.push(new Matrix4f().scale(new Vector3f(2.f / App.get().getWindow().getWidth(), 2.f / App.get().getWindow().getHeight(), 1)));
        if (currentScreen != null) {
            currentScreen.render(renderer);
        }
        renderer.pop(2);
        renderer.flush();
    }

    @Override
    public void onEvent(Event e) {
        if (!enabled)
            return;
        if (currentScreen != null) {
            currentScreen.onEvent(e);
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled)
            return;
        if (currentScreen != null) {
            currentScreen.onUpdate();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
