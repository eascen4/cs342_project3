package app.dto.messages.server;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WaitingForPlayers extends BaseMessage {
    private int requiredPlayers;
    private int queuedPlayers;

    public WaitingForPlayers() {
        super(MessageType.WAITING_FOR_PLAYERS);
    }

    public WaitingForPlayers(int requiredPlayers, int queuedPlayers) {
        this();
        this.requiredPlayers = requiredPlayers;
        this.queuedPlayers = queuedPlayers;
    }
}
