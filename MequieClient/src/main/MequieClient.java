package main;



import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class MequieClient {

	public static void main(String[] args) throws ClassNotFoundException, UnrecoverableKeyException, InvalidKeyException, NumberFormatException, NoSuchAlgorithmException, CertificateException, KeyStoreException, NoSuchPaddingException, SignatureException {
		System.out.println();
		if (args.length != 5) {
			System.err.println("Não inseriu informação suficiente para se ligar ao servidor.\n");
			System.exit(-1);
		}
		MequieClient cliente = new MequieClient();
		String[] address = args[0].split(":"); // address[0] = ip, address[1] = port
		String trustStore = args[1];
		String keyStoreAlias = args[2];
		String keyStorePassword = args[3];
		String username = args[4];
		cliente.connect(address[0], Integer.parseInt(address[1]), trustStore, keyStoreAlias, keyStorePassword, username);
	}

	public void connect(String ip, int port, String trustStore, String keyStoreAlias, String keyStorePassword, String username) throws ClassNotFoundException, NoSuchAlgorithmException, 
	CertificateException, KeyStoreException, UnrecoverableKeyException, NoSuchPaddingException, InvalidKeyException, SignatureException {
		
		System.setProperty("javax.net.ssl.trustStore", trustStore);
		System.setProperty("javax.net.ssl.trustStoreType", "jceks");
		SocketFactory sf = SSLSocketFactory.getDefault();
		try (SSLSocket socket = (SSLSocket) sf.createSocket(ip, port)) {
			ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
			Scanner sc = new Scanner(System.in);
			
			// ---------------- RAID -------------------
			
			// Login Process.
			outStream.writeObject(username); //1
			
			Long nonce = (Long) inStream.readObject(); //2
			
		    FileInputStream keyFile = new FileInputStream(keyStoreAlias);  
		    KeyStore keyStore = KeyStore.getInstance("JCEKS"); //DUVIDA: VERIFICAR QUAL O INSTANCE USADO
		    keyStore.load(keyFile, keyStorePassword.toCharArray());
		    Certificate cert = keyStore.getCertificate(username); 
		    PublicKey pubKey = cert.getPublicKey();
			PrivateKey privKey = (PrivateKey) keyStore.getKey(username, keyStorePassword.toCharArray());
		    
			SignedObject signedObject = new SignedObject(nonce, privKey, Signature.getInstance("MD5withRSA"));
					
			int optionRegister = (int) inStream.readObject(); //3
			
			
			
			if(optionRegister == 1) {
				byte[] certificado = cert.getEncoded();
				outStream.writeObject(certificado);  //4
				
				outStream.writeObject(signedObject); //5
				
			}
			if(optionRegister == 2) {
				
				outStream.writeObject(signedObject);//6
			}
			
			// ---------------- RAID -------------------
			
			boolean loggedIn = (Boolean) inStream.readObject();
			if (!loggedIn) {
				System.err.println("Registo e a autenticação não foram bem-sucedidos!\n");
				sc.close();
				return;
			}

			System.out.println("Bem-vindo " + username + "!\n");
			String[] commands = { "c", "a", "r", "g", "u", "m", "p", "co", "h", "s" };
			while (loggedIn) { // Client main Loop.
				System.out.println("_____________________________________________________________\n");
				System.out.println("--> Criar conversa de grupo: (Escreva c)\n");
				System.out.println("--> Adicionar membro ao grupo: (Escreva a)\n");
				System.out.println("--> Remover membro do grupo: (Escreva r)\n");
				System.out.println("--> Informação do grupo: (Escreva g)\n");
				System.out.println("--> Informacao do utilizador: (Escreva u)\n");
				System.out.println("--> Enviar mensagem para o grupo: (Escreva m)\n");
				System.out.println("--> Enviar fotografia para o grupo: (Escreva p)\n");
				System.out.println("--> Mensagens e fotografias não vistas: (Escreva co)\n");
				System.out.println("--> Histórico de mensagens do grupo: (Escreva h)\n");
				System.out.println("--> Sair: (Escreva s)\n");
				System.out.println("_____________________________________________________________\n");
				String input = sc.nextLine();
				System.out.println();
				boolean valid = false;
				for (int i = 0; i < commands.length; i++) {
					if (input.equals(commands[i])) {
						valid = true;
						break;
					}
				}
				if (!valid) {
					System.err.println("Comando inválido!\n");
				} else {
					outStream.writeObject(input);
					if (input.equals("s")) {
						sc.close();
						loggedIn = false;
						socket.close();
						return;
					}
					switch (input) {
					case "c":
						MequieClientFunctions.createGroup(outStream, inStream, sc, pubKey);
						break;
					case "a":
						MequieClientFunctions.addUser(outStream, inStream, sc);
						break;
					case "r":
						MequieClientFunctions.removeUser(outStream, inStream, sc);
						break;
					case "g":
						MequieClientFunctions.ginfo(outStream, inStream, sc);
						break;
					case "u":
						MequieClientFunctions.uinfo(inStream);
						break;
					case "m":
						MequieClientFunctions.msg(outStream, inStream, sc, privKey);
						break;
					case "p":
						MequieClientFunctions.photo(outStream, inStream, sc, privKey);
						break;
					case "co":
						MequieClientFunctions.collect(outStream, inStream, privKey);
						break;
					case "h":
						MequieClientFunctions.history(outStream, inStream, sc, privKey);
						break;
					}
				}
			}
			sc.close();
		} catch (IOException e) {
			System.err.println("Erro na criação do socket");
			e.printStackTrace();
		}
	}
}
