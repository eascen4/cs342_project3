import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;


public class Client extends Thread{
	
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private final Consumer<Message> callback;
	
	Client(Consumer<Message> callback){
		this.callback = callback;
	}

	public void connect(String host, int port) throws IOException {
		socketClient = new Socket(host, port);
		out = new ObjectOutputStream(socketClient.getOutputStream());
		in = new ObjectInputStream(socketClient.getInputStream());
		socketClient.setTcpNoDelay(true);
		this.start();
	}

	public void run() {
		try {
			while (true) {
				Message message = (Message) in.readObject();
				callback.accept(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void send(Message data) {
		try {
			out.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
