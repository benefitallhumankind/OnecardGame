package onecard;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class User implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9021663186751985297L;
	private transient Socket socket;
	private transient ObjectOutputStream oos;
	private transient ObjectInputStream ois;
	private int port;
	private int cardNum;
	private String name;

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public ObjectOutputStream getOos() {
		return oos;
	}

	public void setOos(ObjectOutputStream oos) {
		this.oos = oos;
	}

	public ObjectInputStream getOis() {
		return ois;
	}

	public void setOis(ObjectInputStream ois) {
		this.ois = ois;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCardNum() {
		return cardNum;
	}

	public void setCardNum(int cardNum) {
		this.cardNum = cardNum;
	}

	public User(Socket socket, int port, String name, ObjectOutputStream oos, ObjectInputStream ois) {
		this.socket = socket;
		this.port = port;
		this.name = name;
		this.oos = oos;
		this.ois = ois;
	}

	@Override
	public String toString() {
		return name + " (" + port + ")";
	}
}