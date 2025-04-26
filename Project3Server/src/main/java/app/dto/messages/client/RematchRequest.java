package app.dto.messages.client;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RematchRequest extends BaseMessage {
    private String gameId;

    public RematchRequest() {
        super(MessageType.REMATCH_REQUEST);
    }

    public RematchRequest(String gameId) {
        this();
        this.gameId = gameId;
    }
}
