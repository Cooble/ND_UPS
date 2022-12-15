package cz.cooble.ndc.gui;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.graphics.BatchRenderer2D;
import cz.cooble.ndc.graphics.font.TextBuilder;
import org.joml.Vector2i;

public class ScreenConnect extends GuiScreen {

    public GuiText lblPlayer;
    public GuiTextBox txtPlayer;

    public GuiText lblIp;
    public GuiTextBox txtIp;
    public GuiButton btnConnect;

    public ScreenConnect() {
        lblPlayer = new GuiText();
        lblPlayer.textAlignment = TextBuilder.ALIGN_CENTER;
        lblPlayer.setText("Player Name:");
        txtPlayer = new GuiTextBox();
        txtPlayer.dim.x = 350;
        txtPlayer.dim.y = 70;
        txtPlayer.maxLength=10;
        txtPlayer.setText("Karel");


        lblIp = new GuiText();
        lblIp.textAlignment = TextBuilder.ALIGN_CENTER;
        lblIp.setText("===\nEnter IP to connect:");
        txtIp = new GuiTextBox();
        txtIp.dim.x = 350;
        txtIp.dim.y = 70;
        txtIp.isValidChar = GuiTextBox.IP_VALIDATOR;
        txtIp.maxLength=21;
        btnConnect = new GuiButton();
        btnConnect.setText("Connect");
        btnConnect.dim.x = 350;
        btnConnect.dim.y = 50;

        addElement(lblPlayer);
        addElement(txtPlayer);
        addElement(lblIp);
        addElement(txtIp);
        addElement(btnConnect);
    }


    @Override
    public void render(BatchRenderer2D renderer) {
        lblIp.pos = new Vector2i(App.get().getWindow().getWidth() / 2,
                App.get().getWindow().getHeight() / 2 + lblIp.dim.y);


        lblPlayer.pos = new Vector2i(App.get().getWindow().getWidth() / 2,
                lblIp.pos.y +lblIp.dim.y+ lblPlayer.dim.y);

        txtPlayer.pos = new Vector2i(App.get().getWindow().getWidth() / 2 - txtPlayer.dim.x / 2,
                lblPlayer.pos.y - lblPlayer.dim.y - txtPlayer.dim.y / 2);


        txtIp.pos = new Vector2i(App.get().getWindow().getWidth() / 2 - txtIp.dim.x / 2,
                lblIp.pos.y - lblIp.dim.y - txtIp.dim.y / 2);

        btnConnect.pos = new Vector2i(App.get().getWindow().getWidth() / 2 - btnConnect.dim.x / 2,
                txtIp.pos.y - btnConnect.dim.y-10);


        super.render(renderer);
    }
}
