package app.dto.messages.server;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatBroadcast extends BaseMessage {
    private String gameId;
    private String senderUsername;
    private String message;

    public ChatBroadcast() {
        super(MessageType.CHAT_NOTIFICATION);
    }

    public ChatBroadcast(String gameId, String senderUsername, String message) {
        this();
        this.gameId = gameId;
        this.senderUsername = senderUsername;
        this.message = message;
    }
}
