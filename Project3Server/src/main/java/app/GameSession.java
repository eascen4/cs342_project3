package app;

import app.dto.GameDto;
import app.dto.messages.client.ChatMessage;
import app.dto.messages.MessageType;
import app.dto.PlayerInfo;
import app.dto.messages.BaseMessage;
import app.dto.messages.server.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class GameSession {

    @Getter
    private final String gameId; // Helps identify the game
    private final Server server;
    @Getter
    private final ConnectFourGame game;

    private final List<ClientRunnable> players;
    private final Map<Integer, ClientRunnable> playersMap; // Player ID -> client
    private final Map<Integer, Boolean> rematchVotes;

    public GameSession(List<ClientRunnable> participants, Server server) {
        this.server = server;
        this.gameId = UUID.randomUUID().toString().substring(0, 8);

        this.players = new ArrayList<>(participants);
        this.playersMap = new ConcurrentHashMap<>();
        this.rematchVotes = new ConcurrentHashMap<>();

        List<Integer> playerIds = new ArrayList<>();
        for (int i = 0; i < participants.size(); i++) {
            int playerId = i + 1; // Assign player IDs 1, 2, 3...
            playerIds.add(playerId);
            ClientRunnable handler = participants.get(i);
            handler.setCurrentGameSession(this); // Link handler to this session
            handler.setPlayerId(playerId); // Store ID in handler
            playersMap.put(playerId, handler);
            rematchVotes.put(playerId, false); // Initialize rematch vote
        }

        this.game = new ConnectFourGame(playerIds);
    }

    public List<ClientRunnable> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized void startGame() {
        game.startGame();

        broadcastGameState();
    }

    public synchronized void handleMove(ClientRunnable requestingClient, int column) {

        if (game.getStatus() != ConnectFourGame.GameStatus.PLAYING) {
            log.warn("Move attempt failed in game {}: Game is not active (Status: {}).", gameId, game.getStatus());
            requestingClient.sendMessage(new ErrorResponse("Game is not currently active."));
            return;
        }

        int playerId = requestingClient.getPlayerId();
        if (playerId != game.getCurrentPlayerId()) {
            log.warn("Move attempt failed in game {}: It's not Player {}'s turn (Current: {}).", gameId, playerId, game.getCurrentPlayerId());
            requestingClient.sendMessage(new ErrorResponse("It's not your turn."));
            return;
        }

        boolean isSuccess = game.dropPiece(playerId, column);

        if(isSuccess) {
            broadcastGameState();

            if(game.getStatus() == ConnectFourGame.GameStatus.FINISHED) {
                handleGameEnd();
            }
        } else {
            log.warn("Move attempt failed in game {}: Player {}'s move in column {} was invalid (rejected by game logic).", gameId, playerId, column);
            requestingClient.sendMessage(new ErrorResponse("Invalid move: Column might be full or invalid."));
        }
    }

    public void handleChatMessage(ClientRunnable sender, ChatMessage chatMessage) {
        String senderUsername = sender.getUsername();

        ChatMessage message = new ChatMessage(this.gameId, senderUsername, chatMessage.getMessage());
        message.setType(MessageType.CHAT_NOTIFICATION);

        for (ClientRunnable player : players) {
            if (player != sender) {
                player.sendMessage(message);
            }
        }
    }

    public synchronized void handleRematchRequest(ClientRunnable requestingClient) {
        if (game.getStatus() != ConnectFourGame.GameStatus.FINISHED) {
            requestingClient.sendMessage(new ErrorResponse("Game is not finished yet."));
            return;
        }

        int playerId = requestingClient.getPlayerId();
        if (rematchVotes.getOrDefault(playerId, false)) {
            requestingClient.sendMessage(new ErrorResponse("You have already voted for a rematch."));
            return;
        }

        log.info("Game {}: Player {} ({}) requests rematch.", gameId, playerId, requestingClient.getUsername());
        rematchVotes.put(playerId, true);

        // Notify other players
        RematchNotification notification = new RematchNotification(requestingClient.getUsername());
        broadcast(notification, requestingClient);

        boolean allVotedYes = rematchVotes.values().stream().allMatch(Boolean::booleanValue);

        if (allVotedYes) {
            log.info("Game {}: Rematch accepted by all players!", gameId);
            resetForRematch();
        } else {
            requestingClient.sendMessage(new RematchRequestUpdate());
        }
    }

    private synchronized void handleGameEnd() {
        log.info("Game {} finished. Status: {}, Winner ID: {}", gameId, game.getStatus(), game.getWinnerId());

        GameEndNotification gameOverMsg = new GameEndNotification();
        gameOverMsg.setGameId(this.gameId);
        gameOverMsg.setStatus((game.getWinnerId() == 0) ? GameDto.GameStatus.DRAW : GameDto.GameStatus.WIN);
        gameOverMsg.setWinnerId(game.getWinnerId());

        // --- ELO Calculation ---
        // TODO: Adapt for 3-4 players if implementing ELO for them
//        if (players.size() == 2) {
//            ClientHandler p1 = players.get(0);
//            ClientHandler p2 = players.get(1);
//            int p1Elo = server.getPlayerDataService().getElo(p1.getUsername());
//            int p2Elo = server.getPlayerDataService().getElo(p2.getUsername());
//            double scoreP1 = (gameOverMsg.winnerId == p1.getPlayerIdInGame()) ? 1.0 : (gameOverMsg.finalStatus == GameDto.GameStatus.DRAW ? 0.5 : 0.0);
//
//            EloService.EloResult result = server.getEloService().calculateNewRatings(p1Elo, p2Elo, scoreP1);
//
//            server.getPlayerDataService().updateElo(p1.getUsername(), result.newRatingPlayerA);
//            server.getPlayerDataService().updateElo(p2.getUsername(), result.newRatingPlayerB);
//
//            // Add ELO changes to the message if the DTO supports it
//            // gameOverMsg.eloChanges = Map.of(
//            //     p1.getUsername(), result.newRatingPlayerA,
//            //     p2.getUsername(), result.newRatingPlayerB
//            // );
//            log.info("Game {}: ELO updated. {}: {} -> {}, {}: {} -> {}", gameId,
//                    p1.getUsername(), p1Elo, result.newRatingPlayerA,
//                    p2.getUsername(), p2Elo, result.newRatingPlayerB);
//        }
        // --- End ELO Calculation ---

        broadcast(gameOverMsg, null);

        server.gameFinished(this);
    }

    private synchronized void resetForRematch() {
        game.resetGame(); // Reset board, status, current player
        rematchVotes.replaceAll((id, v) -> false); // Reset votes
        log.info("Game {} reset for rematch. First turn: Player {}", gameId, game.getCurrentPlayerId());
        broadcastGameState();
    }

    public synchronized void playerLeft(ClientRunnable leavingClient) {
        int leavingPlayerId = leavingClient.getPlayerId();
        log.warn("Game {}: Player {} ({}) left the game.", gameId, leavingPlayerId, leavingClient.getUsername());

        players.remove(leavingClient);
        playersMap.remove(leavingPlayerId);
        rematchVotes.remove(leavingPlayerId);

        // If game was in progress, forfeit
        if (game.getStatus() == ConnectFourGame.GameStatus.PLAYING) {
            game.setStatus(ConnectFourGame.GameStatus.FINISHED);
            // Determine winner
            int winnerId = (players.size() == 1) ? players.getFirst().getPlayerId() : 0; // Simple case
            game.setWinnerId(winnerId);

            log.info("Game {}: Forfeited by Player {}. Winner ID: {}", gameId, leavingPlayerId, winnerId);

            GameEndNotification forfeitMessage = new GameEndNotification();
            forfeitMessage.setGameId(this.gameId);
            forfeitMessage.setStatus(GameDto.GameStatus.FORFEIT);
            forfeitMessage.setWinnerId(winnerId);

            broadcast(forfeitMessage, null);

            // TODO: Handle ELO for forfeits

            // Notify server game ended
            server.gameFinished(this);

        } else {
            // If game was already finished, just log and potentially notify others
            ChatMessage leaveNotification = new ChatMessage(gameId, "Server", leavingClient.getUsername() + " has left the completed game.");
            leaveNotification.setType(MessageType.CHAT_NOTIFICATION);
            broadcast(leaveNotification, null);
        }

        leavingClient.setCurrentGameSession(null);
        leavingClient.setPlayerId(0);
    }

    private void broadcastGameState() {
        GameDto gameStateDto = createGameDto();
        GameUpdate message = new GameUpdate(gameStateDto);
        broadcast(message, null);
    }

    private GameDto createGameDto() {
        GameDto dto = GameDto.builder()
                .gameId(this.gameId)
                .board(game.getBoard())
                .currentPlayerId(game.getCurrentPlayerId())
                .winnerId(game.getWinnerId())
                .build();


        switch(game.getStatus()) {
            case PLAYING:
                dto.setStatus(GameDto.GameStatus.ACTIVE);
                break;
            case FINISHED:
                dto.setStatus((dto.getWinnerId() == 0) ? GameDto.GameStatus.DRAW : GameDto.GameStatus.WIN);
                break;
            default:
                dto.setStatus(GameDto.GameStatus.ACTIVE);
        }

        dto.setPlayers(players.stream()
            .map(p ->
                PlayerInfo.builder()
                        .username(p.getUsername())
                        .status(PlayerInfo.PlayerStatus.IN_GAME)
                        .build()
                )
                .collect(Collectors.toList()));

        return dto;
    }

    private void broadcast(BaseMessage message, ClientRunnable sender) {
        log.debug("Broadcasting in Game {}: {}", gameId, message.getType());
        for (var player : players) {
            if (player != sender) {
                player.sendMessage(message);
            }
        }
    }

}
