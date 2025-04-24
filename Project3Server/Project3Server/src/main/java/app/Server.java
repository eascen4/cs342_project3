package app;

import app.controllers.ServerDashboardController;
import app.dto.messages.MessageType;
import app.dto.messages.BaseMessage;
import app.dto.messages.client.ChatMessage;
import app.dto.messages.client.MatchRequest;
import app.dto.messages.client.MoveRequest;
import app.dto.messages.server.ErrorResponse;
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
	protected void processMessage(BaseMessage message, ClientRunnable client) {
		if(!isRunning) return;


		try {
			switch (message.getType()) {
				case LOGIN_REQUEST:

					break;
				case MATCH_REQUEST:
					break;
				case MOVE_REQUEST:
					break;
				case CHAT_MESSAGE:
					break;
				case REMATCH_REQUEST:
					break;
				case LEAVE_GAME_REQUEST:
					break;
				case CHALLENGE_REQUEST:
					break;
				case CHALLENGE_RESPONSE:
					break;
				default:
					log.warn("Message type {} not supported", message.getType());
			}
		} catch (Exception e) {
			client.sendMessage(new ErrorResponse("Internal Server Error"));
		}
	}

	// --- Message Handlers ---

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

				// Create and start the game session
				GameSession newSession = new GameSession(participants, this);
				activeGames.put(newSession.getGameId(), newSession);
				newSession.startGame();
				broadcastUserListUpdate(); // Update statuses to IN_GAME
				updateUILists();
			} else {
				// Should not happen with drainTo if size check passed, but handle defensively
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


	
	

	
