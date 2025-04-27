package app;

import app.controllers.ServerDashboardController;
import app.dto.PlayerInfo;
import app.dto.messages.MessageType;
import app.dto.messages.BaseMessage;
import app.dto.messages.client.*;
import app.dto.messages.server.ErrorResponse;
import app.dto.messages.server.GameStartNotification;
import app.dto.messages.server.LoginResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


import java.net.ServerSocket;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class Server {
	private static final int DEFAULT_PORT = 5555;
	private final AtomicInteger clientIdCounter = new AtomicInteger(0);
	@Getter
	private volatile boolean isRunning;

	// Server state
	private final Map<Integer, ClientRunnable> clientsById = new ConcurrentHashMap<>();
	private final  Map<String, ClientRunnable> clientsByUsername = new ConcurrentHashMap<>();
	private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
	// Queue for players waiting for match
	private final Map<Integer, BlockingQueue<ClientRunnable>> queuedPlayers = new ConcurrentHashMap<>();

	// Threading
	private ExecutorService clientExecutor;
	private ServerSocket serverSocket;
	private Thread listenerThread;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Setter
	private ServerDashboardController serverDashboardController;
	@Getter
	private final ObservableList<String> observablePlayers = FXCollections.observableArrayList();
	@Getter
	private final ObservableList<String> observableGames = FXCollections.observableArrayList();
	
	Server() {
		queuedPlayers.put(2, new LinkedBlockingQueue<>());
		queuedPlayers.put(3, new LinkedBlockingQueue<>());
		queuedPlayers.put(4, new LinkedBlockingQueue<>());
	}

	public void start() {
		if(isRunning) return;

		log.info("Starting server on port {}", DEFAULT_PORT);
		clientExecutor = Executors.newCachedThreadPool();
		isRunning = true;

		try {
			serverSocket = new ServerSocket(DEFAULT_PORT);
			log.info("Server on port {}", DEFAULT_PORT);

			ConnectionListener listener = new ConnectionListener(serverSocket, this);
			listenerThread = new Thread(listener, "ConnectionListenerThread");
			listenerThread.start();

			log.info("Server started sucessfully and is waiting for clients");
		} catch(Exception e) {
			shutdown();
		}
	}

	public void shutdown() {
		log.info("Shutting down server on port {}", DEFAULT_PORT);
		isRunning = false;

		try {
			if(serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch(Exception e) {
			log.error("Error closing server socket", e);
		}

		if (listenerThread != null && listenerThread.isAlive()) {
			listenerThread.interrupt();
		}

		log.info("Disconnecting all clients...");
		for(var client : clientsById.values()) {
			client.sendMessage(new BaseMessage(MessageType.SERVER_SHUTDOWN));
			client.stopClient();
		}
		clientsById.clear();
		clientsByUsername.clear();
		activeGames.clear();
		queuedPlayers.values().forEach(BlockingQueue::clear);

		updateUILists();

		log.info("Shutting down executor service...");
		clientExecutor.shutdown();
		try {
			if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				log.warn("Executor did not terminate in 5s, forcing shutdown...");
				clientExecutor.shutdownNow(); // Cancel running tasks
				if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
					log.error("Executor did not terminate after forcing.");
				}
			}
		} catch (Exception e) {
			clientExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		log.info("Server has been shutdown");
	}

	protected void handleConnection(Socket socket) {
		if(!isRunning) {
			try {
				 socket.close();
			} catch (Exception ignored) {}
			return;
		}

		int clientId = clientIdCounter.incrementAndGet();
		ClientRunnable clientRunnable = new ClientRunnable(socket, this, clientId);
		clientsById.put(clientId, clientRunnable);
		clientExecutor.submit(clientRunnable);
	}

	protected void removeClient(ClientRunnable runnable) {
		int clientId = runnable.getClientId();
		if(clientsById.remove(clientId) != null) {
			log.info("Client removed with id: {}", clientId);

		} else {
			log.warn("Client not found with id: {}", clientId);
		}

		String username = runnable.getUsername();
		if(username != null) {
			clientsByUsername.remove(username);

			queuedPlayers.values()
					.forEach(queue -> queue.remove(runnable));

			GameSession gameSession = runnable.getCurrentGameSession();
			if(gameSession != null) {
				gameSession.playerLeft(runnable);
			}

			broadcastUserListUpdate();
		}

		updateUILists();
	}

	/**
	 *
	 * @param message - The message object
	 * @param client - client that sent the message (null if server sent)
	 */
	protected void processJsonMessage(String message, ClientRunnable client) {
		if(!isRunning) return;

		BaseMessage baseMsg = null; // To hold the initially parsed message for type info
		try {
			// 1. Parse into BaseMessage just to get the type safely
			baseMsg = objectMapper.readValue(message, BaseMessage.class);
			MessageType type = baseMsg.getType();

			if (type == null) {
				log.error("Received message with null type from client [{}]: {}", client.getClientId(), message);
				client.sendMessage(new ErrorResponse("Invalid message: 'type' field is missing."));
				return;
			}

			// 2. Switch on the type and deserialize DIRECTLY into the specific class
			switch (type) {
				case LOGIN_REQUEST:
					// Deserialize the original JSON string directly to LoginRequest
					LoginRequest loginReq = objectMapper.readValue(message, LoginRequest.class);
					handleLoginRequest(loginReq, client);
					break;
				case MATCH_REQUEST:
					MatchRequest matchReq = objectMapper.readValue(message, MatchRequest.class);
					handleMatchRequest(matchReq, client);
					break;
				case MOVE_REQUEST:
					MoveRequest moveReq = objectMapper.readValue(message, MoveRequest.class);
					handleMoveRequest(moveReq, client);
					break;
				case CHAT_MESSAGE:
					ChatMessage chatMsg = objectMapper.readValue(message, ChatMessage.class);
					handleChatMessage(chatMsg, client);
					break;
				case REMATCH_REQUEST:
					RematchRequest rematchReq = objectMapper.readValue(message, RematchRequest.class);
					handleRematchRequest(rematchReq, client);
					break;
				case LEAVE_GAME_REQUEST:
					LeaveGameRequest leaveReq = objectMapper.readValue(message, LeaveGameRequest.class);
					handleLeaveGame(leaveReq, client);
					break;
//				case CHALLENGE_REQUEST:
//					ChallengeRequest challengeReq = objectMapper.readValue(message, ChallengeRequest.class);
//					handleChallengeRequest(challengeReq, client);
//					break;
//				case CHALLENGE_RESPONSE:
//					ChallengeResponse challengeRes = objectMapper.readValue(message, ChallengeResponse.class);
//					handleChallengeResponse(challengeRes, client);
//					break;
				// Add cases for any other Client -> Server message types

				default:
					log.warn("Received unsupported message type {} from client [{}]: {}", type, client.getClientId(), message);
					client.sendMessage(new ErrorResponse("Unsupported message type: " + type));
			}
		} catch (JsonProcessingException jsonEx) {
			// Error during JSON parsing/deserialization
			log.error("JSON Error processing message from client [{}]: {}", client.getClientId(), jsonEx.getMessage());
			client.sendMessage(new ErrorResponse("Invalid message format: " + jsonEx.getMessage()));
		} catch (Exception e) {
			// Catch other unexpected exceptions during handling
			MessageType typeInfo = (baseMsg != null) ? baseMsg.getType() : MessageType.SERVER_SHUTDOWN; // Use UNKNOWN if initial parse failed
			log.error("!!! Unexpected Internal Server Error processing message type {} for client [{}] !!!",
					typeInfo, client.getClientId(), e); // Log the full stack trace
			client.sendMessage(new ErrorResponse("Internal Server Error processing request. Please contact support."));
		}
	}

	// --- Message Handlers ---

	private synchronized void handleLoginRequest(LoginRequest msg, ClientRunnable handler) {
		String requestedUsername = msg.username;
		log.info("Username: {}", requestedUsername);
		if (handler.getUsername() != null) {
			handler.sendMessage(new ErrorResponse("Already logged in as " + handler.getUsername()));
			return;
		}
		if (requestedUsername == null || requestedUsername.trim().isEmpty() || requestedUsername.length() > 16) {
			handler.sendMessage(new LoginResponse(false, "Invalid username (must be 1-16 chars).", null));
			return;
		}
		// Check uniqueness (case-insensitive)
		if (clientsByUsername.keySet().stream().anyMatch(u -> u.equalsIgnoreCase(requestedUsername))) {
			handler.sendMessage(new LoginResponse(false, "Username '" + requestedUsername + "' is already taken.", null));
			return;
		}

		// Register username
		handler.setUsername(requestedUsername);
		clientsByUsername.put(requestedUsername, handler);

        log.info("User '{}' logged in (Client ID: {})", requestedUsername, handler.getClientId());
		handler.sendMessage(new LoginResponse(true, "Login successful!", requestedUsername));

		broadcastUserListUpdate();
		updateUILists(); // Update server GUI
	}

	private synchronized void handleMatchRequest(MatchRequest msg, ClientRunnable requester) {
		if (requester.getCurrentGameSession() != null) {
			requester.sendMessage(new ErrorResponse("You are already in a game."));
			return;
		}
		int playerCount = msg.getPlayers();
		if (!queuedPlayers.containsKey(playerCount)) {
			requester.sendMessage(new ErrorResponse("Unsupported player count: " + playerCount));
			return;
		}

		BlockingQueue<ClientRunnable> queue = queuedPlayers.get(playerCount);
		log.info("User [" + requester.getUsername() + "] requests " + playerCount + "-player match.");

		// Add requester to queue if not already there
		if (!queue.contains(requester)) {
			queue.offer(requester);
		}

		// Check if enough players are waiting
		if (queue.size() >= playerCount) {
			List<ClientRunnable> participants = new ArrayList<>();
			// Atomically drain the required number of players
			if (queue.drainTo(participants, playerCount) == playerCount) {
				log.info("Found match for " + playerCount + " players: " + participants.stream().map(ClientRunnable::getUsername).collect(Collectors.joining(", ")));

				try {
					// Create the game session - THIS ASSIGNS PLAYER IDs internally
					GameSession newSession = new GameSession(participants, this);
					activeGames.put(newSession.getGameId(), newSession);

					int startingPlayerId = newSession.getGame().getCurrentPlayerId();
					int[][] initialBoard = newSession.getGame().getBoard();

					for (ClientRunnable participant : participants) {
						List<PlayerInfo> opponents = participants.stream()
								.filter(p -> p != participant) // Exclude self
								.map(opponent ->
                                        PlayerInfo.builder()
                                                .status(PlayerInfo.PlayerStatus.IN_GAME)
                                                .username(opponent.getUsername()).build())
								.collect(Collectors.toList());

						// Create the notification tailored for this participant
						GameStartNotification notification = new GameStartNotification(
								newSession.getGameId(),
								opponents,
								participant.getPlayerId(), // The ID assigned by GameSession constructor
								startingPlayerId,
								initialBoard,
								playerCount
						);

						// Send the notification
						participant.sendMessage(notification);
						log.debug("Sent GAME_START to Player {} ({})", participant.getPlayerId(), participant.getUsername());
					}

					newSession.startGame();

					broadcastUserListUpdate();
					updateUILists(); // Update server GUI

				} catch (Exception e) {
					log.error("!!! CRITICAL: Failed to create or start game session after matching players !!!", e);

					participants.forEach(p -> {
						p.sendMessage(new ErrorResponse("Failed to start game after match found. Please try again."));
						queue.offer(p);
					});
				}
			} else {
				log.warn("Could not drain enough players for {}-player match, putting back.", playerCount);
				participants.forEach(queue::offer); // Put them back
				requester.sendMessage(new BaseMessage(MessageType.WAITING_FOR_PLAYERS)); // Define this DTO if needed
			}
		} else {
			log.info("User [" + requester.getUsername() + "] added to " + playerCount + "-player queue.");
			requester.sendMessage(new BaseMessage(MessageType.WAITING_FOR_PLAYERS)); // Inform client they are waiting
		}
	}

	private void handleMoveRequest(MoveRequest msg, ClientRunnable sender) {
		log.info("User [" + sender.getUsername() + "] requests move.");
		GameSession session = sender.getCurrentGameSession();
		if (session == null || !session.getGameId().equals(msg.getGameId())) {
			sender.sendMessage(new ErrorResponse("Invalid game or not in this game."));
			return;
		}
		session.handleMove(sender, msg.getColumn());
	}

	private void handleChatMessage(ChatMessage msg, ClientRunnable sender) {
		if (msg.getGameId() != null) {
			GameSession session = sender.getCurrentGameSession();
			if (session != null && session.getGameId().equals(msg.getGameId())) {
				session.handleChatMessage(sender, msg);
			} else {
				sender.sendMessage(new ErrorResponse("Cannot send chat to game ID " + msg.getGameId()));
			}
		} else { // Lobby chat
			log.debug("Lobby chat from [{}]: {}", sender.getUsername(), msg.getMessage());
			ChatMessage broadcastMsg = new ChatMessage(null, sender.getUsername(), msg.getMessage());
			broadcastMsg.setType(MessageType.CHAT_NOTIFICATION);

			// Send to all players NOT in a game
			clientsByUsername.values().stream()
					.filter(h -> h != sender && h.getCurrentGameSession() == null)
					.forEach(h -> h.sendMessage(broadcastMsg));
		}
	}

	public void handleRematchRequest(RematchRequest msg, ClientRunnable sender) {
		GameSession session = sender.getCurrentGameSession();

		if (session == null) {
			log.warn("Client [{}] sent REMATCH_REQUEST but is not associated with a game session.", sender.getUsername());
			sender.sendMessage(new ErrorResponse("Cannot request rematch: Not in a recently finished game session."));
			return;
		}

		if (msg.getGameId() == null || !msg.getGameId().equals(session.getGameId())) {
			log.warn("Client [{}] sent REMATCH_REQUEST for game {} but is associated with session {}.",
					sender.getUsername(), msg.getGameId(), session.getGameId());
			sender.sendMessage(new ErrorResponse("Rematch request game ID mismatch."));
			return;
		}

		log.info("Forwarding rematch request from [{}] for game {}", sender.getUsername(), session.getGameId());

		session.handleRematchRequest(sender);
	}

	private void handleLeaveGame(LeaveGameRequest msg, ClientRunnable sender) {
		log.info("User [{}] requested to leave.", sender.getUsername());

		GameSession currentSession = sender.getCurrentGameSession();
		if (currentSession != null) {
			log.info("User [{}] is leaving active game session {}", sender.getUsername(), currentSession.getGameId());
			currentSession.playerLeft(sender);

		}

		broadcastUserListUpdate();
		updateUILists();
	}
	// ------------------------

	public void gameFinished(GameSession session) {
        log.info("GameSession {} reported finished.", session.getGameId());
		activeGames.remove(session.getGameId());

		session.getPlayers().forEach(handler -> {

			if (clientsById.containsKey(handler.getClientId())) {
				handler.setCurrentGameSession(null);
				handler.setPlayerId(0);
			}
		});
		broadcastUserListUpdate();
		updateUILists();
	}

	private void broadcastUserListUpdate() {
		List<ClientRunnable> currClients = new ArrayList<>(clientsByUsername.values());

		for(var client : currClients) {
			if(client.getUsername() == null) continue;


		}
		updateUILists();
	}

	private void updateUILists() {
		if(serverDashboardController == null) return;

	}
}


	
	

	
