package app;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
@AllArgsConstructor
public class ConnectionListener implements Runnable {

    private final ServerSocket serverSocket;
    private final Server server;

    @Override
    public void run() {
        log.info("Ready to accept connections. Listening on port {}", serverSocket.getLocalPort());
        while(server.isRunning() && !serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                log.info("Accepted connection from {}", client.getRemoteSocketAddress());

                server.handleConnection(client);
            }
            catch(Exception e) {
                break;
            }
        }
        log.info("Finished accepting connections");
    }
}
