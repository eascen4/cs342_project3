package app.controllers;

import app.GuiClient;
import app.dto.GameDto;
import app.dto.messages.client.ChatMessage;
import app.dto.messages.client.LeaveGameRequest;
import app.dto.messages.client.MoveRequest;
import app.dto.messages.server.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import app.Client;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class GameController implements Initializable {
    @Setter GuiClient guiClient;
    @Setter Client client;

    @FXML private GridPane boardGrid;
    @FXML private ListView<String> listChat;
    @FXML private TextField chatField;
    @FXML private Label turnLabel;
    @FXML private Button leaveGameButton;
    @FXML private Button sendChatButton;

    private String gameId;
    private int myPlayerId;
    private int currentPlayerId;
    private String myUsername;
    private String opponentUsername;
    private Map<Integer, String> playerIdToUsername = new HashMap<>();
    private Map<Integer, Color> playerIdToColor = new HashMap<>();
    private boolean myTurn = false;
    private boolean gameIsOver = false;

    private static final int COLS = 7;
    private static final int ROWS = 6;
    private Circle[][] boardCircles;
    private Node[][] boardClickables;

    private final ObservableList<String> chatList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        boardCircles = new Circle[ROWS][COLS];
        boardClickables = new Node[ROWS][COLS];
        listChat.setItems(chatList);
        setupBoard();


        turnLabel.setText("Waiting for game data...");
        boardGrid.setDisable(true);
    }

    private void setupBoard() {
        boardGrid.getChildren().clear(); // Clear previous game visuals
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Circle circle = new Circle(25);
                circle.setFill(Color.WHITE);
                circle.setStroke(Color.BLACK);
                circle.setStrokeWidth(1.0);
                boardCircles[row][col] = circle;
                GridPane.setHalignment(circle, HPos.CENTER);
                GridPane.setValignment(circle, VPos.CENTER);
                boardGrid.add(circle, col, row);

                Pane clickPane = new Pane();
                clickPane.setPrefSize(55, 55);
                final int column = col;
                clickPane.setOnMouseClicked((event) -> handleColumnClick(column));
                boardClickables[row][col] = clickPane;
                GridPane.setHalignment(clickPane, HPos.CENTER);
                GridPane.setValignment(clickPane, VPos.CENTER);
                boardGrid.add(clickPane, col, row);
            }
        }
        log.debug("Board grid setup complete with {} rows and {} columns.", ROWS, COLS);
    }

    public void initializeGame(GameStartNotification data) {
        if (data == null) {
            log.error("initializeGame called with null data!");
            guiClient.showError("Failed to initialize game: Missing game data.");
            guiClient.showLobby();
            return;
        }
        this.gameId = data.getGameId();
        this.myPlayerId = data.getPlayerId();
        this.currentPlayerId = data.getStartingPlayerId();
        this.gameIsOver = false;
        this.myUsername = client.getCurrUsername(); // Get username from Client/NetworkService

        // Determine opponent username (assuming 1v1 for this simplified version)
        if (data.getOpponents() != null && !data.getOpponents().isEmpty()) {
            this.opponentUsername = data.getOpponents().get(0).getUsername(); // Get first opponent
        } else {
            this.opponentUsername = "Opponent"; // Fallback
            log.warn("Opponent username not found in GameStartNotification for game {}", gameId);
        }

        // Assign colors (Player 1 = Red, Player 2 = Yellow)
        playerIdToColor.clear();
        playerIdToColor.put(1, Color.RED);
        playerIdToColor.put(2, Color.YELLOW);
        // Add more colors if supporting > 2 players later

        playerIdToUsername.clear();
        playerIdToUsername.put(myPlayerId, myUsername);
        // Infer opponent ID for 1v1
        int opponentId = (myPlayerId == 1) ? 2 : 1;
        playerIdToUsername.put(opponentId, opponentUsername);


        log.info("Initializing game {}. My ID: {}, Opponent: {} (ID: {}), Starting ID: {}",
                gameId, myPlayerId, opponentUsername, opponentId, currentPlayerId);

        updateBoard(data.getInitialBoard()); // Display the initial empty board
        updateTurnLabel(); // Set the initial turn label
        chatList.clear(); // Clear chat from previous game
        addChatMessage("System", "Game started vs " + opponentUsername + ". Player " + currentPlayerId + " (" + playerIdToUsername.getOrDefault(currentPlayerId, "???") + ") goes first.");
    }

    public void updateGameState(GameUpdate data) {
        if (gameIsOver || data == null || data.getGameState() == null || !data.getGameState().getGameId().equals(this.gameId)) {
            log.warn("Ignoring game state update for wrong game ({}) or game is over.", data != null && data.getGameState() != null ? data.getGameState().getGameId() : "null");
            return;
        }
        GameDto state = data.getGameState();
        log.debug("Updating game state for game {}. Current player: {}", gameId, state.getCurrentPlayerId());
        this.currentPlayerId = state.getCurrentPlayerId();
        updateBoard(state.getBoard());
        updateTurnLabel();

        if (state.getStatus() != GameDto.GameStatus.ACTIVE) {
            log.warn("Received GameStateUpdate with non-ACTIVE status ({}), processing as game over.", state.getStatus());
            handleGameOver(new GameEndNotification(gameId, state.getStatus(), state.getWinnerId()));
        }
    }

    private void updateBoard(int[][] boardState) {
        if (boardState == null || boardState.length != ROWS || boardState[0].length != COLS) {
            log.error("Received invalid board state dimensions! Cannot update board.");
            return;
        }
        log.trace("Updating board visuals.");
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int playerId = boardState[row][col];
                // Default to white for empty slot (ID 0) or unknown IDs
                Color color = playerIdToColor.getOrDefault(playerId, Color.WHITE);
                if (boardCircles[row][col] != null) {
                    // Ensure update happens on FX thread
                    final int r = row;
                    final int c = col;
                    final Color finalColor = color;
                    Platform.runLater(() -> boardCircles[r][c].setFill(finalColor));
                } else {
                    log.warn("Circle object at [{},{}] is null during board update.", row, col);
                }
            }
        }
    }

    private void updateTurnLabel() {
        if (gameIsOver) return; // Don't update if game finished

        myTurn = (currentPlayerId == myPlayerId);
        String text;
        Color color;

        if (myTurn) {
            text = "Your turn";
            color = Color.GREEN;
        } else {
            // Use opponentUsername directly for 1v1
            text = opponentUsername + "'s turn";
            color = Color.RED;
        }

        final String finalText = text;
        final Color finalColor = color;
        Platform.runLater(() -> {
            turnLabel.setText(finalText);
            turnLabel.setTextFill(finalColor);
            // Disable/enable board interaction based on turn
            boardGrid.setDisable(!myTurn);
            log.trace("Turn label updated: {}. Board disabled: {}", finalText, !myTurn);
        });
    }

    private void handleColumnClick(int column) {
        if (!myTurn || gameIsOver || client == null) {
            log.debug("Column {} clicked, but it's not my turn, game is over, or client is null.", column);
            return;
        }
        log.info("Column {} clicked. Sending move request for game {}.", column, gameId);

        // Send MoveRequest DTO
        client.sendMessage(new MoveRequest(gameId, column));

        myTurn = false;
        updateTurnLabel(); // Show opponent's turn immediately
        boardGrid.setDisable(true);
    }

    public void displayChatMessage(ChatBroadcast chat) {
        // Only display if it's for this game
        if (chat.getGameId() != null && chat.getGameId().equals(this.gameId)) {
            addChatMessage(chat.getSenderUsername(), chat.getMessage());
        } else if (chat.getGameId() == null) {
            log.trace("Ignoring lobby chat message while in game.");
        }
    }

    private void addChatMessage(String sender, String message) {
        String formattedMessage = sender + ": " + message;
        log.debug("Adding chat message: {}", formattedMessage);
        // Ensure UI updates happen on the FX thread
        Platform.runLater(() -> {
            chatList.add(formattedMessage);
            listChat.scrollTo(chatList.size() - 1);
        });
    }

    @FXML
    private void handleSendChat() {
        String message = chatField.getText().trim();
        if (!message.isEmpty() && !gameIsOver && client != null) {
            log.debug("Sending game chat: {}", message);
            // Send ChatMessage DTO with gameId
            client.sendMessage(new ChatMessage(gameId, null, message));
            // Display own message immediately for better UX
            addChatMessage(myUsername, message);
            chatField.clear();
        }
    }

    public void handleGameOver(GameEndNotification data) {
        if (gameIsOver || !data.getGameId().equals(this.gameId)) return; // Already over or wrong game

        log.info("Game Over received for game {}. Status: {}, Winner ID: {}", gameId, data.getStatus(), data.getWinnerId());
        gameIsOver = true;
        boardGrid.setDisable(true); // Disable board

        String resultText;
        Color color;
        if (data.getStatus() == GameDto.GameStatus.WIN) {
            if (data.getWinnerId() == myPlayerId) {
                resultText = "You Win!";
                color = Color.LIMEGREEN;
            } else {
                resultText = opponentUsername + " Wins!";
                color = Color.TOMATO;
            }
        } else { // Draw
            resultText = "It's a Draw!";
            color = Color.ORANGE;
        }

        final String finalText = resultText;
        final Color finalColor = color;
        Platform.runLater(() -> {
            turnLabel.setText(finalText);
            turnLabel.setTextFill(finalColor);
        });
        addChatMessage("System", "Game Over! " + resultText);

        // Automatically switch to results scene via ClientApp
        if (guiClient != null) {
            guiClient.showResultScene(data);
        }
    }

    /** Called by NetworkService (via GuiClient/Platform.runLater) when opponent forfeits */
    public void handleForfeit(ForfeitNotification data) {
        if (gameIsOver || !data.getGameId().equals(this.gameId)) return;

        log.info("Forfeit Notification received for game {}. Player {} forfeited.", gameId, data.getForfeitingUsername());
        gameIsOver = true;
        boardGrid.setDisable(true);

        String message = data.getForfeitingUsername() + " forfeited.";
        Color color = Color.ORANGE;
        if (data.getWinnerId() == myPlayerId) {
            message += " You Win!";
            color = Color.LIMEGREEN;
        } else {
            // This case shouldn't happen in 1v1 if opponent forfeits, but handle defensively
            message += " Game Over.";
        }

        final String finalText = message;
        final Color finalColor = color;
        Platform.runLater(() -> {
            turnLabel.setText(finalText);
            turnLabel.setTextFill(finalColor);
        });
        addChatMessage("System", message);

        // Create a GameOverNotification to pass to result scene
        GameEndNotification gameOverData = new GameEndNotification(gameId, GameDto.GameStatus.FORFEIT, data.getWinnerId());
        if (guiClient != null) {
            guiClient.showResultScene(gameOverData);
        }
    }

    /** Called by NetworkService (via GuiClient/Platform.runLater) when opponent requests rematch */
    public void handleRematchReceived(RematchNotification data) {
        // This notification is likely more relevant on the Result screen
        log.info("Rematch requested by {} (received in GameController).", data.getUsername());
        addChatMessage("System", data.getUsername() + " wants a rematch (respond on results screen).");
    }


    @FXML
    private void handleLeaveGameButtonAction(ActionEvent event) {
        log.info("Leave Game button clicked.");
        if (client == null) return;

        // Ask for confirmation, especially if game isn't over
        String confirmationText = gameIsOver ?
                "Return to the lobby?" :
                "Are you sure you want to leave? This will forfeit the current game.";

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, confirmationText, ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Leave Game");
        confirmAlert.setHeaderText(null);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            log.info("User confirmed leaving game {}.", gameId);
            // Send leave request only if game wasn't already over (to avoid unnecessary forfeit)
            if (!gameIsOver) {
                client.sendMessage(new LeaveGameRequest()); // Assuming DTO takes gameId
            }
            // Go back to lobby immediately
            if (guiClient != null) {
                guiClient.showLobby();
            }
        }
    }

    public boolean isActive() {
        return boardGrid != null && boardGrid.getScene() != null && boardGrid.getScene().getWindow() != null;
    }
}

