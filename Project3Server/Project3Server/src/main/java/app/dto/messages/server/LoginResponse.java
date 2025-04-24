package app.dto.messages.server;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginResponse extends BaseMessage {
    private boolean wasSuccess;
    private String message;
    private String username;

    public LoginResponse() {
        super(MessageType.LOGIN_RESPONSE);
    }

    public LoginResponse(boolean wasSuccess, String message, String username) {
        this();
        this.wasSuccess = wasSuccess;
        this.message = message;
        this.username = username;
    }
}
