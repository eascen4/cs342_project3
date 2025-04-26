package app.dto.messages.client;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChallengeResponse extends BaseMessage {
    private String opponent;
    private boolean accepted;

    public ChallengeResponse() {
        super(MessageType.CHALLENGE_RESPONSE);
    }

    public ChallengeResponse(String opponent, boolean accepted) {
        this();
        this.opponent = opponent;
        this.accepted = accepted;
    }
}
