package app.controllers;

import javafx.application.Platform;
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


import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;


public class MainMenuController implements Initializable {

    @FXML TextField usernameField;
    @FXML Label errorLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        errorLabel.setVisible(false);

        if (ClientData.clientConnection == null) {
            ClientData.clientConnection = new Client(data -> {
                Platform.runLater(() -> {
                    switch (data.getType()) {
                        case LOGIN_RESPONSE:
                            LoginResponse loginResponse = (LoginResponse) data;
                            if (loginResponse.isWasSuccess()) {
                                System.out.println("Login success!");
                                errorLabel.setVisible(false);
                                ClientData.username = loginResponse.getUsername();
                                goToLobby();
                            } else {
                                System.out.println("Login failed :(");
                                errorLabel.setText(loginResponse.getMessage());
                                errorLabel.setVisible(true);
                            }
                            break;
                    }
                });
            });
            try {
                ClientData.clientConnection.connect("127.0.0.1", 5555);
            } catch (Exception  e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleConnect() {
        String username = usernameField.getText();
        if (!username.isEmpty()) {
            LoginRequest loginAttempt = new LoginRequest(username);
            ClientData.clientConnection.send(loginAttempt);
        }
    }

    @FXML
    private void handleQuit() {
        Platform.exit();
    }

    private void goToLobby() {
        if (ClientData.sceneMap.containsKey("lobby")) {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(ClientData.sceneMap.get("lobby"));
        }
    }
}