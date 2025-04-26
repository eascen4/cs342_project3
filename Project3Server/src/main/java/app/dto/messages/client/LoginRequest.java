package app.dto.messages.client;

import app.dto.messages.MessageType;
import app.dto.messages.BaseMessage;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginRequest extends BaseMessage {
    public String username;

    public LoginRequest() {
        super(MessageType.LOGIN_REQUEST);
    }

    public LoginRequest(String username) {
        this();
        this.username = username;
    }
}
