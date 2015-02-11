package dictionaryLookUpNetParallel;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LookUpClient {

	private String ip = "";
	private int port = 4444;
	private Socket clientSocket = null;

	public LookUpClient() {
		super();
	}

	public LookUpClient(String ip, int port) {

		super();
		this.ip = ip;
		this.port = port;
		clientSocket = new Socket();
		try {
			clientSocket.connect(new InetSocketAddress(this.ip, this.port));
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		
		try {

			String ip = InetAddress.getLocalHost().getHostAddress();
		    int port = 4444;
			if (args.length > 0) {

				if (args.length > 1) {

					ip = args[0];
					port = Integer.parseInt(args[1]);

				}
				else {

					if (args[0].contains("."))
						ip = args[0];
					else
						port = Integer.parseInt(args[0]);

				}

			}

			LookUpClient luc = new LookUpClient(ip, port);
			//luc.runClient(clientSocket, IPAddress, port);

		}
		catch (IOException e) {
            e.printStackTrace();
		}

	}

}