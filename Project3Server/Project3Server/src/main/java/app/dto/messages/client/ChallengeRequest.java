package app.dto.messages.client;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChallengeRequest extends BaseMessage {
    private String opponent;

    public ChallengeRequest() {
        super(MessageType.CHALLENGE_REQUEST);
    }

    public ChallengeRequest(String opponent) {
        this();
        this.opponent = opponent;
    }
}
