package app;

import app.dto.messages.BaseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

@Slf4j
@ToString(of = {"clientId", "username"})
@EqualsAndHashCode()
public class ClientRunnable implements Runnable{

    private final Socket socket;
    @Getter
    private final int clientId;

    private BufferedReader in;
    private PrintWriter out;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Server server;

    @Getter @Setter
    private String username;
    @Getter @Setter
    private GameSession currentGameSession;
    @Getter @Setter
    private int playerId;

    public ClientRunnable(Socket socket, Server server, int clientId) {
        this.socket = socket;
        this.server = server;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        log.info("Client started running with ID: {}", clientId);

        try (
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        ) {

            this.in = in;
            this.out = out;
            socket.setTcpNoDelay(true);

            String line;
            while((line = in.readLine()) != null) {
                try {
                    server.processJsonMessage(line, this);
                } catch(Exception e) {
                    log.error("Could not parse message from client [{}]. Class not found.", clientId, e);
                    break;
                }
            }
        } catch(SocketException e) {
            log.error("Client [{}] disconnected ({})", clientId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error when reading message from client [{}].", clientId, e);
        }

        log.info("Removing connection from client {}", clientId);
        server.removeClient(this);
        log.info("Client finished running with ID: {}", clientId);
    }

    public synchronized void sendMessage(BaseMessage message) {
        try {
            String json = mapper.writeValueAsString(message);
            out.println(json);
            log.debug("Message sent to client {}: {}", clientId, message);
        } catch (Exception e) {
            log.warn("Oops something went wrong sending to client {}: {}", clientId, e.getMessage());
        }
    }

    public void stopClient() {
        try {
            if(socket != null && !socket.isClosed()) socket.close();
            log.info("Client stopped");
        } catch (IOException e) {
            log.warn("Error when closing socket on client {}: {}", clientId, e.getMessage());
        }
    }

}
