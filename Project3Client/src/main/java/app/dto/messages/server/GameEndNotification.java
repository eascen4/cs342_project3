package app.dto.messages.server;

import app.dto.GameDto;
import app.dto.PlayerInfo;
import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class GameEndNotification extends BaseMessage {
    private String gameId;
    private GameDto.GameStatus status;
    private int winnerId;

    public GameEndNotification() {
        super(MessageType.GAME_END);
    }

    public GameEndNotification(String gameId, GameDto.GameStatus status, int winnerId) {
        this();
        this.gameId = gameId;
        this.status = status;
        this.winnerId = winnerId;
    }
}
