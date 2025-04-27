package app.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import app.ClientData;
import app.Client;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import app.dto.messages.client.RematchRequest;


import java.net.URL;
import java.util.ResourceBundle;


public class ResultController implements Initializable {
    @FXML private Button rematchButton;
    @FXML private Button returnMainButton;
    @FXML private Label resultLabel;

    private String gameResult;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateResult(ClientData.gameResult);
        if (ClientData.gameResult != null) {
            switch (ClientData.gameResult) {

            }
        }
    }

    private void updateResult(String result) {
        if (result == null) return;

        switch (result.toUpperCase()) {
            case "WIN":
                resultLabel.setText("You Win!");
                resultLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                break;
            case "LOSE":
                resultLabel.setText("You Lose!");
                resultLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                break;
            case "DRAW":
                resultLabel.setText("It's a Draw!");
                resultLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                break;
        }
        ClientData.gameResult = result;
    }

    @FXML
    private void handleRematch() {
        RematchRequest rematchRequest = new RematchRequest(ClientData.gameId);
        // ClientData.clientConnection.send(rematchRequest);
        rematchButton.setDisable(true);
        rematchButton.setText("Waiting for opponent...");
    }

    @FXML
    private void handleReturnMain() {
        if (ClientData.sceneMap.containsKey("main")) {
            Stage stage = (Stage) returnMainButton.getScene().getWindow();
            stage.setScene(ClientData.sceneMap.get("main"));
        }
    }




}