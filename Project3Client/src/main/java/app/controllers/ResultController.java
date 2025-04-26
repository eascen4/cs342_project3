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


public class ResultController implements Initializable {
    @FXML private Button rematchButton;
    @FXML private Button returnLobbyButton;
    @FXML private Button returnMainButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ClientData.clientConnection == null) {
            ClientData.clientConnection = new Client(data -> {
                Platform.runLater(() -> {
                    switch (data.type) {

                    }
                });
            });
            try {
                ClientData.clientConnection.connect("127.0.0.1", 5555);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleRematch() {
        // if the other client also wants to play again, then both go backt to game scene
    }

    @FXML
    private void handleReturnMain() {
        if (ClientData.sceneMap.containsKey("main")) {
            Stage stage = (Stage) returnMainButton.getScene().getWindow();
            stage.setScene(ClientData.sceneMap.get("main"));
        }
    }




}