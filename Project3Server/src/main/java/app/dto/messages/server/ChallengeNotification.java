package app.dto.messages.server;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChallengeNotification extends BaseMessage {
    private String challengerUsername;

    public ChallengeNotification() {
        super(MessageType.CHALLENGE_NOTIFICATION);
    }

    public ChallengeNotification(String challengerUsername) {
        this();
        this.challengerUsername = challengerUsername;
    }
}
