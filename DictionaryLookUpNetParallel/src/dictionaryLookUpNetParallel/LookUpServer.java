package dictionaryLookUpNetParallel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class LookUpServer {

	private static final int MAX_THREADS = (Runtime.getRuntime().availableProcessors() - 1);
	private static final int MAX_CLIENTS = (MAX_THREADS * 10);

	private volatile String ip = "";
	private volatile int port = 4444;
	private ServerSocket serverSocket = null;
	private volatile boolean isRunning = false;
	private HashMap<String, Socket> userSockets = null;
	private ArrayList<ClientThread> threadPool = null;
	private Queue<String> jobs = null;

	public static enum MSG_TYPE {

		WELCOME(0),
		CONFIRM(1),
		NORMAL(2),
		TERMINATE(3);

		private final int value;
		private MSG_TYPE(int value) {
			this.value = value;
		}

		public int getValue() {

			return this.value;

		}

	}

	private class ClientThread extends Thread {

		private volatile boolean isRunning = false;
		private LookUpServer parrent = null;
		private volatile String word = "";
		private volatile String clientName = "";

		public ClientThread() {
			super();
		}

		public ClientThread(LookUpServer lus) {

			super();
			parrent = lus;

		}

		@Override
		public void run() {
			
			isRunning = true;
			while (isRunning && getParrentIsRunning()) {

				while (clientName.equals("") && word.equals("") && isRunning && getParrentIsRunning()) {

					try {
						sleep(100l);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
				if (isRunning && getParrentIsRunning() && !word.equals("") && !clientName.equals("")) {

					PrintWriter output = null;					
					//TODO: process word and send results back to client
					synchronized(this) {

						this.word = "";
						this.clientName = "";
						//output.close();
						output = null;

					}

				}

			}
			
		}

		public boolean getIsRunning() {
			return isRunning;
		}
		public void setIsRunning(boolean isRun) {
			isRunning = isRun;
		}

		public void processWord(String clientName, String clientWord) {
			synchronized(this) {
				if (this.clientName.equals("") && this.word.equals("")) {
					this.clientName = clientName;
					this.word = clientWord;
				}
			}
		}
		public boolean isProcessingWord() {
			synchronized(this) {
				return (!clientName.equals("") && !word.equals(""));
			}
		}

		private boolean getParrentIsRunning() {
			synchronized(parrent) {
				return parrent.getIsRunning();
			}
		}

	}

	public LookUpServer() {
		super();
	}

	public LookUpServer(String ip, int port) {

		super();
		this.ip = ip;
		this.port = port;
		try {
			serverSocket = new ServerSocket();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocket.bind(new InetSocketAddress(this.ip, this.port));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		userSockets = new HashMap<String, Socket>(MAX_CLIENTS);
		jobs = new LinkedList<String>();
		threadPool = new ArrayList<ClientThread>(MAX_THREADS);
		for (int i = 0; i < MAX_THREADS; i++) {
			threadPool.add(new ClientThread(this));
			threadPool.get(i).start();
		}

	}

	public boolean getIsRunning() {
		return isRunning;
	}
	public void setIsRunning(boolean isRun) {
		isRunning = isRun;
	}

	public void runServer() {

		isRunning = true;
		while (isRunning) {

			synchronized(userSockets) {
				if (userSockets.size() >= MAX_CLIENTS) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
						continue;
					}
					continue;
				}
			}

			Socket newClientSocket = null;
			try {
				newClientSocket = serverSocket.accept();
			}
			catch (IOException e) {
				continue;
			}
			PrintWriter output = null;
			BufferedReader input = null;
			try {

				output = new PrintWriter(newClientSocket.getOutputStream());
			
				output.print(MSG_TYPE.WELCOME.getValue() + ":" + "Welcome to the server, please send your username\r\n");
				output.flush();

				input = new BufferedReader(new InputStreamReader(newClientSocket.getInputStream()));

				boolean validUsername = true;
				int count = 0;
				String line = "";
				do {

					line = input.readLine();
					if (line == null) {
	
						input.close();
						output.close();
						break;
	
					}
					line = line.trim();
					if (!line.split(":")[0].equals(Integer.toString(MSG_TYPE.CONFIRM.getValue()))) {
	
						output.print(MSG_TYPE.TERMINATE.getValue() + ":" + "Invalid message\r\n");
						output.flush();
						output.close();
						input.close();
						break;
	
					}
					line = line.split(":")[1];
					synchronized(userSockets) {
						for (String user : userSockets.keySet())
							if (user.toUpperCase().equals(line.toUpperCase())) {
	
								validUsername = false;
								output.print(MSG_TYPE.WELCOME.getValue() + ":" + "Invalid username\r\n");
								output.flush();
	
							}
					}

				} while ((++count) < 3 && !validUsername);
				if (count >= 3 || !validUsername) {

					input.close();
					output.close();
					continue;

				}

				output.print(MSG_TYPE.CONFIRM.getValue() + ":" + "Username confirmed as " + line + "\r\n");
				output.flush();

				synchronized(userSockets) {

					userSockets.put(line, newClientSocket);
					jobs.add(line);

				}
				input.close();
				output.close();

			}
			catch (IOException e) {
				continue;
			}

		}

	}

	public static void main(String[] args) {

		try {

			String ip = InetAddress.getLocalHost().getHostAddress();
		    int port = 4444;
		    File file = Paths.get(System.getProperty("user.dir"), "dictionary.txt").toFile();
		    String filePath = "";

			if (args.length > 0) {

			    if (args.length > 1) {

			    	if (args[0].contains(".") && args[0].contains(":")) {

			    		ip = args[0].split(":")[0];
			    		port = Integer.parseInt(args[0].split(":")[1]);
			    		filePath = args[1];

			    	}
			    	else if (args[0].contains(".") && !args[0].contains(":")) {

			    		ip = args[0];
			    		filePath = args[1];

			    	}
			    	else if (!args[0].contains(".") && args[0].contains(":")) {

			    		port = Integer.parseInt(args[0].split(":")[1]);
			    		filePath = args[1];

			    	}
			    	else {

			    		ip = args[1].split(":")[0];
			    		port = Integer.parseInt(args[1].split(":")[1]);
			    		filePath = args[0];

			    	}

				}
				else {

					if (args[0].contains(".") && args[0].contains(":")) {

			    		ip = args[0].split(":")[0];
			    		port = Integer.parseInt(args[0].split(":")[1]);

			    	}
			    	else if (args[0].contains(".") && !args[0].contains(":")) {

			    		ip = args[0];

			    	}
			    	else if (!args[0].contains(".") && args[0].contains(":")) {

			    		port = Integer.parseInt(args[0].split(":")[1]);

			    	}
			    	else {

			    		filePath = args[0];

			    	}

				}

			}

			if (!filePath.equals(""))
				file = new File(filePath);

			if (file.exists() && !file.isDirectory() && file.isFile() && file.canRead()) {

				LookUpServer luc = new LookUpServer(ip, port);
				//TODO: start job handeler thread
				luc.runServer();

			}
			else {

				System.err.println("Error: " + file.getPath() + " does not exists, it is not a regular file, or it cannont be read.");
				System.exit(1);

			}

		}
		catch (IOException e) {
            e.printStackTrace();
		}

	}

}