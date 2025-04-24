package app.dto.messages.client;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatMessage extends BaseMessage {
    private String gameId;
    private String senderUsername;
    private String recipientUsername;
    private String message;

    public ChatMessage() {
        super(MessageType.CHAT_MESSAGE);
    }

    public ChatMessage(String message) {
        this();
        this.message = message;
    }

    public ChatMessage(String gameId, String recipientUsername, String message) {
        this();
        this.gameId = gameId;
        this.recipientUsername = recipientUsername;
        this.message = message;
    }
}
