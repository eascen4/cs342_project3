package app;

import app.dto.messages.BaseMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;
import app.dto.messages.BaseMessage;


public class Client extends Thread{
	
	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	private final Consumer<BaseMessage> callback;
	
	public Client(Consumer<BaseMessage> callback){
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
				BaseMessage message = (BaseMessage) in.readObject();
				callback.accept(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void send(BaseMessage data) {
		try {
			out.writeObject(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
