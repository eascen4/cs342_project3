package app.controllers;

import app.GuiClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import app.ClientData;
import app.Client;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import app.dto.messages.server.LoginResponse;
import app.dto.messages.client.LoginRequest;
import lombok.Setter;


import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;


public class LoginController implements Initializable {
    @Setter GuiClient guiClient;
    @Setter Client client;

    @FXML Label errorLabel;

    @FXML TextField usernameField;
    @FXML TextField hostField;
    @FXML TextField portField;

    @FXML Button connectButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hostField.setText("127.0.0.1");
        portField.setText("5555");
    }

    @FXML
    private void handleConnectButton(ActionEvent event) {
        String username = usernameField.getText().trim();
        String host = hostField.getText().trim();
        String portString = portField.getText().trim();

        if (username.isEmpty()) {
            setMessage("Username cannot be empty.", true);
            return;
        }
        if (host.isEmpty()) {
            setMessage("Host cannot be empty.", true);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portString);
            if (port <= 0 || port > 65535) throw new NumberFormatException("Port out of range");
        } catch (NumberFormatException e) {
            setMessage("Invalid port number.", true);
            return;
        }

        setIsConnecting(true);
        setMessage("Connecting to " + host + ":" + port, false);

        boolean isConnected = client.connect(host, port);

        if (isConnected) {
            client.sendMessage(new LoginRequest(username));
            setMessage("Connected. Logging in as '" + username + "'...", false);
        } else {
            setIsConnecting(false); // enable UI
        }
    }

    public void handleLoginResponse(LoginResponse response) {
        setIsConnecting(false);
        if(response.isWasSuccess()) {
            setMessage("Successfully logged in.", true);
            guiClient.showLobby();
        } else {
            setMessage("Failed to log in.", true);
        }
    }

    @FXML
    private void handleQuit() {
        Platform.exit();
    }

    public void setMessage(String message, boolean isError) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(message != null && !message.isEmpty());
            errorLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        });
    }

    public void resetUI() {
        Platform.runLater(() -> {
            setIsConnecting(false);
            setMessage("", false);
        });
    }

    private void setIsConnecting(boolean isConnecting) {
        Platform.runLater(() -> {
            connectButton.setDisable(isConnecting);
            usernameField.setDisable(isConnecting);
            hostField.setDisable(isConnecting);
            portField.setDisable(isConnecting);
        });
    }
}