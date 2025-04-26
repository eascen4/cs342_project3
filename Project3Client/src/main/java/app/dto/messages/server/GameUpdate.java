package app.dto.messages.server;

import app.dto.GameDto;
import app.dto.messages.MessageType;
import app.dto.messages.BaseMessage;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class GameUpdate extends BaseMessage {
    private GameDto gameState;

    public GameUpdate() {
        super(MessageType.GAME_UPDATE);
    }

    public GameUpdate(GameDto gameState) {
        this();
        this.gameState = gameState;
    }
}

