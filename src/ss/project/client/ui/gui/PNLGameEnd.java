package ss.project.client.ui.gui;

import ss.project.client.Controller;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fw on 26/01/2017.
 */
public class PNLGameEnd extends GUIPanel {
    private JLabel informationLabel;

    public PNLGameEnd(Controller controller) {
        this.setLayout(new BorderLayout());

        this.add(GUIUtils.createLabel("End", GUIUtils.LabelType.TITLE), BorderLayout.NORTH);
        informationLabel = GUIUtils.createLabel("", GUIUtils.LabelType.TITLE);
        this.add(informationLabel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            if (controller.isConnected()) {
                controller.switchTo(Controller.Panel.MULTI_PLAYER_LOBBY);
            } else {
                controller.switchTo(Controller.Panel.SINGLE_PLAYER_SETTINGS);
            }
        });
        bottomPanel.add(backButton);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void onEnter() {
        int winner = Controller.getController().getEngine().getWinner();
        switch (Controller.getController().getEngine().getWinReason()) {
            case WINLENGTHACHIEVED: {
                informationLabel.setText(Controller.getController().getEngine()
                        .getPlayer(winner).getName() + " won the game!");
                break;
            }
            case BOARDISFULL: {
                informationLabel.setText("Draw!");
                break;
            }
            case PLAYERDISCONNECTED: {
                informationLabel.setText(Controller.getController().getEngine().
                        getPlayer(winner).getName() + " disconnected... :(");
                break;
            }
            case GAMETIMEOUT: {
                informationLabel.setText("Someone took too long to do their turn.");
                break;
            }
        }
    }

    @Override
    public void onLeave() {

    }
}
