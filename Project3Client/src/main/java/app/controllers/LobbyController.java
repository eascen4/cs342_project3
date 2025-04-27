package app.controllers;

import app.Client;
import app.GuiClient;
import app.dto.messages.client.MatchRequest;
import app.dto.messages.server.WaitingForPlayers;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class LobbyController implements Initializable {

    @Setter private GuiClient guiClient;
    @Setter private Client client;

    @FXML private Button joinButton;
    @FXML private Button returnButton;
    @FXML private Label statusLabel;

    private boolean isSearching = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.debug("LobbyController initializing...");
        resetUI();
    }

    public void requestInitialData() {
        log.debug("Lobby is active.");
        resetUI(); // Ensure UI is reset when entering
    }

    @FXML
    void handleJoin(ActionEvent event) {
        if (isSearching || client == null) {
            log.warn("Join clicked but already searching or client is null.");
            return;
        }

        log.info("Join Random User button clicked.");
        isSearching = true;
        setSearchingState(true);
        setStatusMessage("Searching for an opponent...");

        client.sendMessage(new MatchRequest(2));
    }

    @FXML
    void handleReturn(ActionEvent event) {
        log.info("Return to Main Menu button clicked.");
        if (isSearching) {
            log.info("Was searching, cancelling matchmaking (if server supports it).");
            isSearching = false; // Reset state locally regardless
        }
        if (guiClient != null) {
            guiClient.showLoginScreen("Returned from Lobby.");
        }
    }

    public void showWaitingMessage(WaitingForPlayers data) {
        // This confirms we are in the queue
        log.debug("Received waiting message from server.");
        isSearching = true; // Ensure state is correct
        setSearchingState(true); // Keep UI disabled
        setStatusMessage("Waiting for opponent (" + data.getQueuedPlayers() + "/" + data.getRequiredPlayers() + ")...");
    }

    /**
     * Resets the UI state, e.g., when first entering the lobby
     * or after a game finishes and returns here.
     */
    public void resetUI() {
        Platform.runLater(() -> {
            isSearching = false;
            setSearchingState(false);
            clearStatusMessage();
        });
    }

    // --- Helper Methods ---

    /** Updates the status label text and visibility */
    private void setStatusMessage(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setVisible(message != null && !message.isEmpty());
        });
    }

    /** Clears the status label */
    private void clearStatusMessage() {
        setStatusMessage("");
    }

    /** Enables/disables buttons based on searching state */
    private void setSearchingState(boolean searching) {
        Platform.runLater(() -> {
            joinButton.setDisable(searching);
        });
    }

    /** Called by ClientApp to check if this scene is active */
    public boolean isActive() {
        return joinButton != null && joinButton.getScene() != null && joinButton.getScene().getWindow() != null;
    }
}
