package app.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

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
                    switch (data.type) {
                        case LOGIN_SUCCESS:
                            System.out.println("Login success!");
                            errorLabel.setVisible(false);
                            ClientData.username = usernameField.getText();
                            goToLobby();
                            break;

                        case LOGIN_FAILURE:
                            System.out.println("Login failed :(");
                            errorLabel.setVisible(true);
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
            Message loginAttempt = new Message(username, MessageType.LOGIN_ATTEMPT);
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