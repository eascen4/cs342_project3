package app.dto.messages.server;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
public class ForfeitNotification extends BaseMessage {
    private String gameId;
    private String forfeitingUsername;
    private int winnerId;

    public ForfeitNotification() {
        super(MessageType.FORFEIT_NOTIFICATION);
    }

    public ForfeitNotification(String gameId, String forfeitingUsername, int winnerId) {
        this();
        this.gameId = gameId;
        this.forfeitingUsername = forfeitingUsername;
        this.winnerId = winnerId;
    }
}
