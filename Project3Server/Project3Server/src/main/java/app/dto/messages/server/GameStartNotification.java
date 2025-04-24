package app.dto.messages.server;

import app.dto.PlayerInfo;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GameStartNotification extends BaseMessage {
    private String gameId;
    private List<PlayerInfo> opponents;
    private int playerId;
    private int startingPlayerId;
    private int[][] initialBoard;
    private int gameType;

    public GameStartNotification() {
        super(MessageType.GAME_START);
    }

    public GameStartNotification(String gameId, List<PlayerInfo> opponents, int playerId, int startingPlayerId, int[][] initialBoard, int gameType) {
        this();
        this.gameId = gameId;
        this.opponents = opponents;
        this.playerId = playerId;
        this.startingPlayerId = startingPlayerId;
        this.initialBoard = initialBoard;
        this.gameType = gameType;
    }
}
