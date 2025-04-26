package app.dto.messages.client;

import app.dto.messages.BaseMessage;
import app.dto.messages.MessageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveGameRequest extends BaseMessage {
    public LeaveGameRequest() {
        super(MessageType.LEAVE_GAME_REQUEST);
    }
}
