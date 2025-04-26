package app.dto.messages.server;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RematchRequestUpdate extends BaseMessage {
    public RematchRequestUpdate() {
        super(MessageType.REMATCH_REQUEST_UPDATE);
    }
}
