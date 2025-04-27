package app.controllers;

import app.GuiClient;
import app.dto.messages.server.GameEndNotification;
import app.dto.messages.server.RematchNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import app.Client;
import app.dto.messages.client.RematchRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class ResultController implements Initializable {
    @Setter GuiClient guiClient;
    @Setter Client client;

    @FXML private Button rematchButton;
    @FXML private Button returnButton;

    @FXML private Label resultLabel;

    private String gameId;
    private int playerId;
    private boolean rematchRequestByMe = false;
    private boolean opponentRequestRematch = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resetState();
    }

    public void initializeData(GameEndNotification data, int playerId) {
        if (data == null) {
            log.error("ResultController initialized with null data!");
            resultLabel.setText("Game Over (Unknown Result)");
            this.gameId = null;
            this.playerId = playerId;
            rematchButton.setDisable(true);
            return;
        }

        this.gameId = data.getGameId();
        log.info("Initializing Result scene for game {}. Status: {}", gameId, data.getStatus());
        updateResultLabel(data);
        resetState();
    }

    private void updateResultLabel(GameEndNotification data) {
        String text;
        Color textColor = Color.WHITE;
        String style = "-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 3, 0, 0, 1);";

        switch (data.getStatus()) {
            case WIN:
                if (data.getWinnerId() == playerId) {
                    text = "You Win!";
                    textColor = Color.LIMEGREEN;
                } else {
                    text = "You Lose!";
                    textColor = Color.TOMATO;
                }
                break;
            case DRAW:
                text = "It's a Draw!";
                textColor = Color.ORANGE;
                break;
            case FORFEIT:
                if (data.getWinnerId() == playerId) {
                    text = "Opponent Forfeited - You Win!";
                    textColor = Color.LIMEGREEN;
                } else {
                    text = "You Forfeited - You Lose!";
                    textColor = Color.TOMATO;
                }
                break;
            default:
                text = "Game Over";
                textColor = Color.LIGHTGRAY;
        }

        final String finalText = text;
        final Color finalColor = textColor;
        Platform.runLater(() -> {
            resultLabel.setText(finalText);
            resultLabel.setStyle(style + "-fx-text-fill: " + toWebColor(finalColor) + ";");
        });
    }

    @FXML
    private void handleRematch() {
        if (gameId == null || client == null) {
            log.warn("Cannot request rematch, gameId or client is null.");
            return;
        }
        if (rematchRequestByMe) return;

        log.info("Requesting rematch for game {}", gameId);
        client.sendMessage(new RematchRequest(gameId));
        rematchRequestByMe = true;
        updateRematchButtonState();
    }

    @FXML
    private void handleReturnToLobby() {
        if(guiClient == null) return;
        guiClient.showLobby();
    }

    // --------------------- Exterior Handlers ---------------------------
    public void handleRematchReceived(RematchNotification data) {
        log.info("Rematch request received from opponent: {}", data.getUsername());
        opponentRequestRematch = true;
        updateRematchButtonState();

        Platform.runLater(() -> {
            Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
            infoAlert.setTitle("Rematch Offer");
            infoAlert.setHeaderText(data.getUsername() + " wants a rematch!");
            infoAlert.setContentText("Click 'Request Rematch' if you also want to play again.");
            infoAlert.show();
        });
    }

    public void handleRematchAcknowledged() {
        log.debug("Server acknowledged rematch request.");
        updateRematchButtonState();
    }

    //--------------------------------------------------------------------

    private void updateRematchButtonState() {
        Platform.runLater(() -> {
            if (rematchRequestByMe && opponentRequestRematch) {
                rematchButton.setText("Rematch Accepted!");
                rematchButton.setDisable(true);
            } else if (rematchRequestByMe) {
                rematchButton.setText("Waiting for Opponent...");
                rematchButton.setDisable(true);
            } else if (opponentRequestRematch) {
                rematchButton.setText("Accept Rematch Request");
                rematchButton.setDisable(false);
            } else {
                rematchButton.setText("Request Rematch");
                rematchButton.setDisable(false);
            }
            returnButton.setDisable(rematchRequestByMe && opponentRequestRematch);
        });
    }

    private void resetState() {
        rematchRequestByMe = false;
        opponentRequestRematch = false;
        updateRematchButtonState();
    }

    private String toWebColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public boolean isActive() {
        return resultLabel != null && resultLabel.getScene() != null && resultLabel.getScene().getWindow() != null;
    }
}