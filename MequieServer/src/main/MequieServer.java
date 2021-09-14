package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class MequieServer {
	private ServerSocket serverSocket;
	private Socket[] connectionSockets = new Socket[1024];
	private int numOfClients = 0;

	public static void main(String[] args) {
		System.out.println();
		MequieServer server = new MequieServer();
		if(args.length != 3) {
			System.err.println("Os dados inseridos não são válidos!\n");
			System.exit(-1);
		}
		server.startServer(args);
	}

	private void startServer(String[] parameters) {
		int port = Integer.parseInt(parameters[0]);
		String keyStoreAlias = parameters[1];
		String keyStorePassword = parameters[2];
		System.setProperty("javax.net.ssl.keyStore", keyStoreAlias);
		System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
		System.setProperty("javax.net.ssl.keyStoreType", "jceks");
		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();

		try {

			SSLServerSocket sSoc = (SSLServerSocket) ssf.createServerSocket(port);
			System.out.println("O servidor ligou-se.\n");
			destructionDetection();
			while(true) {
				int i = 0;
				Socket connectionSocket = sSoc.accept();
				connectionSockets[numOfClients] = connectionSocket;
				numOfClients++;
				System.out.println("Ligação " + numOfClients + " aceite" + "\n");
				i++;
				MequieServerThread thread = new MequieServerThread(connectionSocket);
				new Thread(thread).start();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void destructionDetection() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					Thread.sleep(200);
					System.out.println("A fechar...\n");
					if(MequieServer.this.connectionSockets[0] == null) {
						System.out.println("Sem ligações para terminar. Fechar servidor.\n");
						return;
					}
					for(int i = 0; i < MequieServer.this.numOfClients;i++) {
						connectionSockets[i].close();
						System.out.println("Ligação " + i + " terminada.\n");
					}
					if(MequieServer.this.serverSocket != null)
						MequieServer.this.serverSocket.close();
					System.out.println("Sockets foram fechados. Terminar\n");
					return; //Code to indicate forced termination (Ctrl + c)
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					e.printStackTrace();
				} catch (IOException e) {
					Thread.currentThread().interrupt(); 
					e.printStackTrace();
				}
			}
		});
	}
}
