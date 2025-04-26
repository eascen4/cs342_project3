package app.dto.messages.server;

import app.dto.messages.MessageType;
import app.dto.messages.BaseMessage;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse extends BaseMessage {
    private String message;

    public ErrorResponse() {
        super(MessageType.ERROR_RESPONSE);
    }

    public ErrorResponse(String message) {
        this();
        this.message = message;
    }

}
