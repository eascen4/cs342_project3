package app.dto.messages.client;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MatchRequest extends BaseMessage {
    private int players;

    public MatchRequest() {
        super(MessageType.MATCH_REQUEST);
    }

    public MatchRequest(int players) {
        this();
        this.players = players;
    }
}
