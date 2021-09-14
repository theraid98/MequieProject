package main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.NoSuchElementException;
import java.util.Random;

import catalogs.GroupCatalog;
import catalogs.UserCatalog;
import exceptions.IllegalGroupOperationException;
import exceptions.NoSuchGroupException;
import exceptions.NoSuchUserException;
import utilities.User;

public class MequieServerThread implements Runnable {
	private Socket socket;
	private UserCatalog userCatalog;
	private User currentUser;
	private long nonce;
	protected byte[] serverKey;

	public MequieServerThread(Socket connectionSocket) {
		socket = connectionSocket;
		userCatalog = UserCatalog.getCatalog();
		nonce = new Random().nextLong();
	}

	@Override
	public void run() {
		try {
			ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
			
			boolean pubKeys = new File("PubKeys").mkdir(); //Criar diretorio da PubKeys
			
			
			String username = (String) inStream.readObject(); //1
			
			boolean loggedIn = false;
			try {
				outStream.writeObject(nonce);//2
				loggedIn = authenticate(username, outStream, inStream);
			} catch (InvalidKeyException | CertificateException | SignatureException | NoSuchAlgorithmException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} //1
			
			

			if (!loggedIn) {
				System.out.println("-> O utilizador falhou o login.\n");
				outStream.writeObject(false);
				return;
			}
			
			
			
			outStream.writeObject(true);

			System.out.println("-> O utilizador " + username + " autenticou-se.\n");

			while (loggedIn) {
				String action = (String) inStream.readObject();
				try {
					switch (action) {
					case "c":
						MequieServerFunctions.createGroup(outStream, inStream, currentUser);
						break;
					case "a":
						MequieServerFunctions.addUser(outStream, inStream, currentUser);
						break;
					case "r":
						MequieServerFunctions.removeUser(outStream, inStream, currentUser);
						break;
					case "g":
						MequieServerFunctions.ginfo(outStream, inStream, currentUser);
						break;
					case "u":
						MequieServerFunctions.uinfo(outStream, currentUser);
						break;
					case "m":
						MequieServerFunctions.msg(outStream, inStream, currentUser);
						break;
					case "co":
						MequieServerFunctions.collect(outStream, inStream, currentUser);
						break;
					case "h":
						MequieServerFunctions.history(outStream, inStream, currentUser);
						break;
					case "p":
						MequieServerFunctions.photo(outStream, inStream, currentUser);
						break;
					case "s":
						System.out.println("-> O utilizador " + username + " desconectou-se.\n");
						this.socket.close();
						return;
					}
				} catch (NoSuchUserException e) {
					System.out.println(e.getMessage());
					outStream.writeObject(-1);
				} catch (IllegalGroupOperationException e) {
					System.out.println(e.getMessage());
					outStream.writeObject(-2);
				} catch (NoSuchGroupException e) {
					System.out.println(e.getMessage());
					outStream.writeObject(-3);
				} catch (NoSuchElementException e) {
					System.out.println("-> O utilizador não pertence a este grupo.\n");
					outStream.writeObject(-4);
				}
			}
			System.out.println("-> O utilizador " + username + " desconectou-se.\n");
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

	}

	private boolean authenticate(String username, ObjectOutputStream outputStream, ObjectInputStream inputStream) throws IOException, ClassNotFoundException, CertificateException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (userCatalog.containsUser(username)) {
			outputStream.writeObject(2);
			if(logIn(username, inputStream)) { //verificar depois esta parte
				return true;
			}
			return false;
		} else {							//ele nao esta registado
			outputStream.writeObject(1);  //3
			byte[] cert = (byte[]) inputStream.readObject(); //4
			Certificate certificado = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(cert));
		    PublicKey ku = certificado.getPublicKey(); 		//DUVIDA: ASSUMIMOS QUE O FICHEIRO DO CERTIFICADO JA EXISTE OU TENHO DE O ENVIAR PELO SOCKET?
			
		    SignedObject signedObject = (SignedObject) inputStream.readObject(); //5
		    
		    Signature signature = Signature.getInstance("MD5withRSA");
		    boolean verification = signedObject.verify(ku, signature);
		    
		    if(verification) {
		    	Long nonceCliente = (Long) signedObject.getObject(); //5
		    	if(nonceCliente  != this.nonce) {
		    		return false;
		    	}
		    	register(username, certificado);    //NOME DO CERTIFICADO, COMO FICA???
				return true;
		    }else {
		    	return false;
		    }	
		}
	}

	private boolean logIn(String username,  ObjectInputStream inputStream) throws ClassNotFoundException, IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		User user = userCatalog.getUser(username);
//		if (user.authenticate(password)) {
//			currentUser = user;
//			return true;
//		}
		SignedObject signedObject = (SignedObject) inputStream.readObject(); //6
		//DUVIDA: NAO SEI SE ESTA CORRETO ASSIM, PARA IR BUSCAR AO FICHEIRO, PORQUE O NOME/PAHT (USER) PODE ESTAR MAL
		Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(user.getCertificate())); 
		PublicKey userKey = cert.getPublicKey();
		
		Signature signature = Signature.getInstance("MD5withRSA");
	    boolean verification = signedObject.verify(userKey, signature);
		
		if(verification) {
			currentUser = user;
			return true;
		}
		
		return false;
	}

	private void register(String username, Certificate certificado) throws FileNotFoundException {
		
		userCatalog.registerUser(username, certificado);
		User u = userCatalog.getUser(username);
		currentUser = userCatalog.getUser(username);
		System.out.println("-> O utilizador " + username + " registou-se.\n");
		//groupCatalog.getGroup(0).addUser(u);
		System.out.println("-> O utilizador " + username + " foi adicionado ao grupo geral.\n");
	}
}
