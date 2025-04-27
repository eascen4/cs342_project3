package app.controllers;

import app.GuiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import app.ClientData;
import app.Client;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Setter;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class GameController implements Initializable {
    @Setter GuiClient guiClient;
    @Setter Client client;

    @FXML private GridPane boardGrid;
    @FXML private ListView<String> listChat;
    @FXML private TextField chatField;
    @FXML private Label turnLabel;
    @FXML private Button returnMainButton;

    private String gameId;
    private int myPlayerId;
    private int currentPlayerId;
    private Map<Integer, String> playerIdToUsername = new HashMap<>();
    private Map<Integer, Color> playerIdToColor = new HashMap<>();
    private boolean myTurn = false;
    private boolean gameIsOver = false;

    private static final int COLS = 7;
    private static final int ROWS = 6;
    private Circle[][] board;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        board = new Circle[COLS][ROWS];
        setupBoard();
    }

    private void setupBoard() {
        boardGrid.getChildren().clear();
        for(int row= 0; row < ROWS; row++) {
            for(int col = 0; col < COLS; col++) {
                Circle circle = new Circle(25);

            }
        }
    }

    private void updateTurnLabel() {
        if (myTurn) {
            turnLabel.setText("Your turn");
            turnLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            turnLabel.setText("Opponent's turn");
            turnLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleReturnMain() {
        if (ClientData.sceneMap.containsKey("main")) {
            Stage stage = (Stage) returnMainButton.getScene().getWindow();
            stage.setScene(ClientData.sceneMap.get("main"));
        }
    }

    @FXML
    private void handleSendChat() {
        String message = chatField.getText();
        if (!message.isEmpty()) {
            // send chat
            chatField.clear();
        }
    }
}

