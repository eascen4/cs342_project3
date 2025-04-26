package app.dto.messages.client;

import app.dto.messages.MessageType;
import app.dto.messages.BaseMessage;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class MoveRequest extends BaseMessage {
    private String gameId;
    private int column;

    public MoveRequest() {
        super(MessageType.MOVE_REQUEST);
    }

    public MoveRequest(String gameId, int column) {
        this();
        this.gameId = gameId;
        this.column = column;
    }
}
