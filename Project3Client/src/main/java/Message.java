import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;
    MessageType type;
    String message;
    int recipient;

    public Message(int i, boolean connect){ // for server(?)
        if(connect) {
            type = MessageType.NEWUSER;
            message = "User "+i+" has joined!";
            recipient = i;
        } else {
            type = MessageType.DISCONNECT;
            message = "User "+i+" has disconnected!";
            recipient = i;
        }
    }

    public Message(String mess){
        type = MessageType.TEXT;
        message = mess;
        recipient = -1;
    }

    public Message(String username, MessageType type) { // for MainMenuController
        this.message = username;
        this.type = type;
        this.recipient = -1;
    }

    public Message(MessageType type) { // for LobbyController
        this.type = type;
        this.message = "";
        this.recipient = -1;
    }

    public Message(int rec, String mess){
        type = MessageType.TEXT;
        message = mess;
        recipient = rec;
    }
}

