package app.dto.messages.server;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RematchNotification extends BaseMessage {
    private String username;

    public RematchNotification() {
        super(MessageType.REMATCH_NOTIFICATION);
    }

    public RematchNotification(String username) {
        this();
        this.username = username;
    }

}
