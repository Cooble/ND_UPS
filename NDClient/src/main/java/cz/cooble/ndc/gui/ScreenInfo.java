package cz.cooble.ndc.gui;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.graphics.font.TextBuilder;
import org.joml.Vector2i;

public class ScreenInfo extends GuiScreen {

    public GuiText lblInfo;
    public GuiButton btnOk;

    public ScreenInfo() {
        lblInfo = new GuiText();
        lblInfo.textAlignment = TextBuilder.ALIGN_CENTER;
        lblInfo.setText("Info");
        btnOk = new GuiButton();
        btnOk.setText("Ok");
        btnOk.dim.x = 350;
        btnOk.dim.y = 50;

        addElement(lblInfo);
        addElement(btnOk);
    }
    public void setInfo(String s){
        lblInfo.setText(s);
    }


    @Override
    public void render(BatchRenderer2D renderer) {
        lblInfo.pos = new Vector2i(App.get().getWindow().getWidth() / 2,
                App.get().getWindow().getHeight() / 2 + lblInfo.dim.y);

        btnOk.pos = new Vector2i(App.get().getWindow().getWidth() / 2 - btnOk.dim.x / 2,
                lblInfo.pos.y-lblInfo.dim.y - btnOk.dim.y-10);

        super.render(renderer);
    }
}
