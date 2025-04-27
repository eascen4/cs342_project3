package app;

import app.dto.messages.BaseMessage;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import app.dto.messages.server.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Client {

	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "ClientListenerThread");
		t.setDaemon(true);
		return t;
	});
	
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private volatile boolean isRunning = false;

	@Setter private GuiClient guiClient;

	@Getter ConnectionState connectionState = ConnectionState.DISCONNECTED;
	@Getter String currUsername;

	private ObjectMapper objectMapper = new ObjectMapper();
	public enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, VERIFIED}

	public boolean connect(String host, int port) {
		connectionState = ConnectionState.CONNECTING;
		try {
			socket = new Socket(host, port);
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

			isRunning = true;
			connectionState = ConnectionState.CONNECTED;
			executor.submit(this::listenToServer);
			return true;
		} catch (IOException e) {
			cleanup();
			connectionState = ConnectionState.DISCONNECTED;
			Platform.runLater(() -> {
				if (guiClient != null) {
					guiClient.showError("Connection Failed: " + e.getMessage());
					// if (guiClient.getLoginController() != null) guiClient.getLoginController().resetUI();
				}
			});
			return false;
		}
	}

	private void handleJsonMessage(String jsonMessage) {
		if (guiClient == null) {
			log.error("guiClient reference is null. Cannot process message: {}", jsonMessage);
			return;
		}

		BaseMessage baseMsg = null; // To hold the initially parsed message for type info
		try {
			// 1. Parse minimally just to get the type safely
			baseMsg = objectMapper.readValue(jsonMessage, BaseMessage.class);
			MessageType type = baseMsg.getType();

			if (type == null) {
				log.error("Received message with null type: {}", jsonMessage);
				guiClient.showError("Invalid message from server: 'type' field is missing.");
				return;
			}

			log.debug("Handling server message type: {}", type);

			// 2. Switch on the type and deserialize DIRECTLY into the specific class
			switch (type) {
				case LOGIN_RESPONSE:
					// Deserialize the original JSON string directly to LoginResponse
					LoginResponse loginResponse = objectMapper.readValue(jsonMessage, LoginResponse.class);
					if (loginResponse.isWasSuccess()) {
						connectionState = ConnectionState.VERIFIED;
						this.currUsername = loginResponse.getUsername();
					}
					// Delegate to the currently active LoginController
					if (guiClient.getLoginController() != null) {
						guiClient.getLoginController().handleLoginResponse(loginResponse);
					}
					break;
				case GAME_START:
					GameStartNotification gameStart = objectMapper.readValue(jsonMessage, GameStartNotification.class);
					guiClient.showGameScene(gameStart);
					break;
//				case GAME_UPDATE:
//					if (guiClient.getGameController() != null && guiClient.isGameSceneActive()) {
//						GameStateUpdate gameState = objectMapper.readValue(jsonMessage, GameStateUpdate.class);
//						guiClient.getGameController().updateGameState(gameState);
//					}
//					break;
//				case GAME_END:
//					if (guiClient.getGameController() != null && guiClient.isGameSceneActive()) {
//						GameOverNotification gameOver = objectMapper.readValue(jsonMessage, GameOverNotification.class);
//						guiClient.getGameController().handleGameOver(gameOver);
//						// Trigger showing results scene from guiClient
//						guiClient.showResultScene(gameOver);
//					}
//					break;
//				case CHAT_NOTIFICATION:
//					ChatBroadcast chat = objectMapper.readValue(jsonMessage, ChatBroadcast.class);
//					// Route chat to lobby or game controller based on current scene
//					if (guiClient.isGameSceneActive() && guiClient.getGameController() != null) {
//						guiClient.getGameController().displayChatMessage(chat);
//					} else if (guiClient.isLobbySceneActive() && guiClient.getLobbyController() != null) {
//						guiClient.getLobbyController().displayChatMessage(chat);
//					}
//					break;
				case ERROR_RESPONSE:
					ErrorResponse error = objectMapper.readValue(jsonMessage, ErrorResponse.class);
					guiClient.showError(error.getMessage());
					break;
//				case CHALLENGE_NOTIFICATION:
//					if (guiClient.getLobbyController() != null && guiClient.isLobbySceneActive()) {
//						ChallengeReceived challenge = objectMapper.readValue(jsonMessage, ChallengeReceived.class);
//						guiClient.getLobbyController().handleChallengeReceived(challenge);
//					}
//					break;
//				case REMATCH_NOTIFICATION:
//					// Can be received in Game or Result scene
//					if (guiClient.isGameSceneActive() && guiClient.getGameController() != null) {
//						RematchReceived rematchGame = objectMapper.readValue(jsonMessage, RematchReceived.class);
//						guiClient.getGameController().handleRematchReceived(rematchGame);
//					} else if (guiClient.isResultSceneActive() && guiClient.getResultController() != null) {
//						RematchReceived rematchResult = objectMapper.readValue(jsonMessage, RematchReceived.class);
//						guiClient.getResultController().handleRematchReceived(rematchResult); // Add this method to ResultController
//					}
//					break;
//				case WAITING_FOR_PLAYERS:
//					if (guiClient.getLobbyController() != null && guiClient.isLobbySceneActive()) {
//						WaitingForPlayersMessage waiting = objectMapper.readValue(jsonMessage, WaitingForPlayersMessage.class);
//						guiClient.getLobbyController().showWaitingMessage(waiting);
//					}
//					break;
//				case FORFEIT_NOTIFICATION:
//					if (guiClient.getGameController() != null && guiClient.isGameSceneActive()) {
//						ForfeitNotification forfeit = objectMapper.readValue(jsonMessage, ForfeitNotification.class);
//						guiClient.getGameController().handleForfeit(forfeit);
//					}
//					break;
//				case REMATCH_REQUEST_UPDATE:
//					// Can be received in Game or Result scene
//					if (guiClient.isGameSceneActive() && guiClient.getGameController() != null) {
//						// GameController might show "Waiting..."
//					} else if (guiClient.isResultSceneActive() && guiClient.getResultController() != null) {
//						guiClient.getResultController().handleRematchAcknowledged();
//					}
//					break;
				case SERVER_SHUTDOWN:
					handleDisconnect();
					break;
				// Add cases for other Server -> Client message types

				default:
					log.warn("Unhandled message type received from server: {}", type);
			}
		} catch (JsonProcessingException jsonEx) {
			// Error during JSON parsing/deserialization into specific DTO
			log.error("JSON Error processing message from server: {}", jsonEx.getMessage());
			guiClient.showError("Invalid message format received from server: " + jsonEx.getMessage());
		} catch (Exception e) {
			// Catch other unexpected exceptions during handling
			MessageType typeInfo = (baseMsg != null) ? baseMsg.getType() : MessageType.SERVER_SHUTDOWN;
			log.error("!!! Unexpected Internal Client Error processing message type {} !!!", typeInfo, e);
			guiClient.showError("Internal Client Error processing server message. Please restart.");
		}
	}

	public synchronized void sendMessage(BaseMessage message) {
		if(connectionState == ConnectionState.DISCONNECTED || out == null) return;

		try {
			String json = objectMapper.writeValueAsString(message);
			out.println(json);
		} catch(Exception e) {

		}
	}

	public void disconnect() {
		if(connectionState == ConnectionState.DISCONNECTED) return;

		cleanup();
		Platform.runLater(this::handleDisconnect);
		if(!executor.isShutdown()) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	private void handleDisconnect() {
		if(connectionState == ConnectionState.DISCONNECTED) return;

		connectionState = ConnectionState.DISCONNECTED;
		this.currUsername = null;

		if(guiClient != null) guiClient.showLoginScreen("BAck to Login");
	}

	private synchronized void cleanup() {
		isRunning = false;

		try {
			if (in != null) in.close();
		} catch (IOException e) {
			log.trace("Error closing input stream: {}", e.getMessage());
		}
		if (out != null) out.close();
		try {
			if (socket != null && !socket.isClosed()) socket.close();
		} catch (IOException e) {
			log.trace("Error closing socket: {}", e.getMessage());
		}

		in = null;
		out = null;
		socket = null;

		log.info("Clean up done");
	}

	/**
	 * Connection Thread function
	 */
	private void listenToServer() {
		log.debug("Listener thread started.");
		// Use local variables for streams within try-with-resources
		try (BufferedReader reader = this.in) { // Assuming 'in' is initialized in connect()
			if (reader == null) {
				log.error("Input stream is null in listener thread. Cannot listen.");
				return;
			}

			String jsonLine;
			while (isRunning && (jsonLine = reader.readLine()) != null) {
				log.trace("Received raw JSON from server: {}", jsonLine);
				final String finalJsonLine = jsonLine;
				try {
					Platform.runLater(() -> handleJsonMessage(finalJsonLine));

				} catch (Exception e) {
					log.error("Unexpected error before dispatching JSON processing: {}", finalJsonLine, e);
				}
			} // End of while loop
		} catch (SocketException | EOFException e) {
			if (isRunning) log.warn("Connection closed by server or network error: {}", e.getMessage());
			else log.info("Listener stopped due to intentional disconnect.");
		} catch (IOException e) {
			if (isRunning) log.error("IOException reading from server.", e);
		} catch (Exception e) { // Catch any other unexpected errors
			log.error("Unexpected error in listener thread", e);
		}
		finally {
			log.debug("Listener thread exiting.");
			// Ensure disconnection state is handled on FX thread if not already disconnected
			if (connectionState != ConnectionState.DISCONNECTED) {
				Platform.runLater(this::handleDisconnect);
			}
		}
	}
	//--------------------------------------------------------------
}
