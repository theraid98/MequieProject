package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class MequieClientFunctions {

	protected static void createGroup(ObjectOutputStream outStream, ObjectInputStream inStream, Scanner sc, PublicKey pubKey) throws IOException, ClassNotFoundException {
		boolean valid = false;

		while (!valid) {

			System.out.println("-> Insira um ID para o grupo.\n");
			int groupID = getGroupID(sc);
			System.out.println();
			outStream.writeObject(groupID); //1

			valid = (boolean) inStream.readObject(); //2 && 3
			if (!valid) {
				System.err.println("-> O ID que inseriu não é válido.\n");
			}
		}

		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			SecretKey sharedKey = keyGen.generateKey();
			Cipher ci = Cipher.getInstance("RSA");
			ci.init(Cipher.WRAP_MODE, pubKey);
			byte[] wrappedKey = ci.wrap(sharedKey);
			outStream.writeObject(wrappedKey.length);//4
			outStream.write(wrappedKey, 0, wrappedKey.length); //5
			outStream.flush();
				System.out.println("-> Grupo criado com sucesso!\n");

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
			e.printStackTrace();
		}
	}

	protected static void addUser(ObjectOutputStream outStream, ObjectInputStream inStream, Scanner sc) throws IOException, ClassNotFoundException {
		System.out.println("-> Quem pretende adicionar? (Escreva o nome de utilizador)\n");
		String toAdd = sc.nextLine();
		System.out.println();
		outStream.writeObject(toAdd); //1

		int valid = (int) inStream.readObject(); //2
		if (valid != 2) {
			printErrorMessage(valid);
			return;
		}

		System.out.println("-> A que grupo pretende adicionar " + toAdd + "? (Escreva o ID do grupo)\n");
		int groupID = getGroupID(sc);
		System.out.println();
		outStream.writeObject(groupID); //3
		int error = (int)inStream.readObject(); //4
		if(error != 1)
			printErrorMessage(error);

		List<String> usersName = (List<String>)inStream.readObject(); //5  -> Change to new keys

		sendNewKeys(usersName, outStream);


		valid = (int) inStream.readObject();
		if (valid != 1) {
			printErrorMessage(valid);
		} else {
			System.out.println("-> " + toAdd + " adicionado ao grupo " + groupID + ".\n");
		}
	}
	
	
	private static void sendNewKeys(List<String> userNames, ObjectOutputStream outStream) throws IOException {
		
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			SecretKey sharedKey = keyGen.generateKey();
			for(String user : userNames) {
				File file = new File(".." + File.separator + "PubKeys" + File.separator + user + ".cer");  //VERIFICAR A PATH
				FileInputStream certificate = new FileInputStream(file);
				Certificate cert = null;	// VERIFICAR SE POSSO POR A NULL OU OUTRA COISA
				try {
					cert = CertificateFactory.getInstance("X.509").generateCertificate(certificate);
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				PublicKey p = cert.getPublicKey();
				Cipher ci = Cipher.getInstance("RSA");
				ci.init(Cipher.WRAP_MODE, p);
				byte[] wrappedKey = ci.wrap(sharedKey);
				outStream.writeObject(wrappedKey.length);
				outStream.write(wrappedKey, 0, wrappedKey.length);  //6
				outStream.flush();
			}
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
			e.printStackTrace();
		}
	}
	
	
	
	

	protected static void removeUser(ObjectOutputStream outStream, ObjectInputStream inStream, Scanner sc) throws IOException, ClassNotFoundException {
		System.out.println("-> Quem pretende remover do grupo? (Escreva o nome de utilizador)\n");
		String toRemove = sc.nextLine();
		System.out.println();
		outStream.writeObject(toRemove); //1
		int valid = (int) inStream.readObject(); //2
		if (valid != 2) {
			printErrorMessage(valid);
			return;
		}
		System.out.println("-> De que grupo pretende remover " + toRemove + "? (Escreva o ID do grupo)\n");
		int groupID = getGroupID(sc);
		System.out.println();
		outStream.writeObject(groupID); //3
		
		List<String> usersName = (List<String>)inStream.readObject(); //5

		sendNewKeys(usersName, outStream);

		
		valid = (int) inStream.readObject();
		if (valid != 1) {
			printErrorMessage(valid);
		} else {
			System.out.println("-> " + toRemove + " foi removido do grupo " + groupID + ".\n");
		}
	}

	protected static void ginfo(ObjectOutputStream out, ObjectInputStream in, Scanner sc) throws IOException, ClassNotFoundException {
		int y = (int) in.readObject();
		if (y != 2) {
			printErrorMessage(y);
			return;
		}

		System.out.println("-> De que grupo pretende ver a informação? (Escreva o ID do grupo)\n");
		int groupID = getGroupID(sc);
		out.writeObject(groupID);

		int x = (int) in.readObject();
		if (x != 1) {
			printErrorMessage(x);
			return;
		}
		String s = (String) in.readObject();
		System.out.println(s);
	}

	protected static void uinfo(ObjectInputStream in) throws ClassNotFoundException, IOException {
		System.out.println("-> A sua informacao é:\n");
		String info = (String) in.readObject();
		System.out.println(info);

	}

	protected static void msg(ObjectOutputStream out, ObjectInputStream in, Scanner sc, PrivateKey privKey) throws ClassNotFoundException, IOException {
		System.out.println("-> Para que grupo quer enviar a mensagem? (Escreva o ID do grupo)\n");
		int groupID = getGroupID(sc);
		System.out.println();
		out.writeObject(groupID); //1

		int ans = (int) in.readObject(); //2
		if (ans != 2) {
			printErrorMessage(ans);
			return;
		}

		byte[] groupKey = (byte[]) in.readObject(); //3 -> Chave mais recente do Grupo.
		
		Cipher ci = cifraDeGrupo(groupKey, privKey);
		
		System.out.println("-> Que mensagem quer enviar?\n");
		String message = sc.nextLine();

		
		try {
			byte[] msg = ci.doFinal(message.getBytes());
			out.writeObject(msg.length);
			out.write(msg,0,msg.length);
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	//4 -> Enviar em byte[] a cifra com a mensagem
		
		out.flush(); 
		System.out.println("-> Mensagem enviada\n");

	}

	protected static void photo(ObjectOutputStream outStream, ObjectInputStream inStream, Scanner sc, PrivateKey privKey) {
		try {
			System.out.println("-> Para que grupo quer enviar a foto? (Escreva o ID do grupo)\n");
			int groupID = getGroupID(sc);
			System.out.println();
			outStream.writeObject(groupID); //1

			int x = (int) inStream.readObject(); //2
			if (x != 2) {
				printErrorMessage(x);
				return;
			}

			System.out.println("-> Qual é o caminho para a fotografia? (Exemplo 'foto.<extensao do ficheiro>)\n");
			String path = sc.nextLine();
			System.out.println();

			File photo = new File(path);
			if (!path.contains(".")) {
				System.err.println("-> Não colocou a extensão da fotografia!\n");
				return;
			}
			if (!"jpegjpgpng".contains(path.split("\\.")[1])) {
				System.err.println("Ficheiro inserido não é suportado!\n");
				return;
			}
			if (!photo.exists()) {
				System.err.println("A fotografia não existe!\n");
				return;
			}
			
			
			
			byte[] groupKey = (byte[]) inStream.readObject(); //3 -> Chave mais recente do Grupo.
			
			Cipher ci = cifraDeGrupo(groupKey, privKey);
			
			System.out.println("-> Insira o nome da fotografia.\n");
			String photoName = sc.next() + "." + path.split("\\.")[1];
			sc.nextLine();
			System.out.println();
			outStream.writeObject(photoName); //4

			long fileSize = photo.length(); 
			byte[] buffer = new byte[1024];
			
			outStream.writeObject(fileSize); //5
			outStream.flush();
			
			FileInputStream fileInput = new FileInputStream(photo);
			CipherInputStream cipherInput = new CipherInputStream(fileInput, ci); 
			
			for (int i = 0; i < fileSize;) {
				int toWrite = cipherInput.read(buffer, 0, (int) Math.min(buffer.length, fileSize - i));
				if (toWrite > 0) {
					i += toWrite;
				}
				
				outStream.write(buffer, 0, toWrite);
				outStream.flush();
			}
			fileInput.close();
			cipherInput.close();
			int y = (int) inStream.readObject();
			if (y == 1) {
				System.out.println("-> Ficheiro enviado com sucesso.\n");
			}
		} catch (IOException | ClassNotFoundException e ) {
			e.printStackTrace();
		}
	}
	
	protected static void collect(ObjectOutputStream out, ObjectInputStream in, PrivateKey privKey) {

		try {
			StringBuilder str = new StringBuilder();
			int received = (int) in.readObject(); //Leitura 1.
			while (received == 5) {
				int groupId = (int)in.readObject(); //Id do grupo.
							
				//Receber boolean para começar ciclo das mensagens
				boolean ciclo = (boolean) in.readObject();
				if(ciclo)
					str.append("Grupo" + groupId + ":\n");
				while(ciclo) {
					int keySize = (int)in.readObject();
					byte[] chave = new byte[keySize];
					in.read(chave,0,keySize);
					Cipher ci = decifraDeGrupo(chave,privKey);
					int file = (int) in.readObject();
					if(file == 1000) {
						getPicture(out, in, privKey, ci);
						str.append("Foto\n");
					}else {
						int msgSize = (int)in.readObject();
						byte[] msgCifrada = new byte[msgSize];
						in.read(msgCifrada,0,msgSize);
						
						//TESTAR:
						String s = new String(ci.doFinal(msgCifrada));
						String writer = (String)in.readObject();
						str.append(writer + ": " + s + "\n");
					}
					ciclo = (boolean) in.readObject();
				}

				
				received = (int) in.readObject();
			}
			if (received != 3) {
				printErrorMessage(received);
				return;
			}
			if(str.length() == 0)
				str.append((String)in.readObject());
			System.out.println(str.toString());
			
		} catch (IOException | ClassNotFoundException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
	}

	protected static void history(ObjectOutputStream out, ObjectInputStream in, Scanner sc, PrivateKey privKey) throws IOException, ClassNotFoundException {
		System.out.println("-> De que grupo pretende ver o histórico? (Escreva o ID do grupo)\n");
		int groupID = getGroupID(sc);
		System.out.println();
		out.writeObject(groupID);

		Integer x = (int) in.readObject();
		if (x != 1) {
			printErrorMessage(x);
			return;
		}
		
		int received = (int) in.readObject();
		StringBuilder str = new StringBuilder();
		while(received == 5) {
			int keySize = (int)in.readObject();
			byte[] cifraKey = new byte[keySize];
			in.read(cifraKey,0,keySize);
			Cipher ci = decifraDeGrupo(cifraKey, privKey);
			int msgSize = (int)in.readObject();
			byte[] msgCifrada = new byte[msgSize];
			in.read(msgCifrada,0,msgSize);
			String escritor = (String)in.readObject();
			//TESTAR:
			try {
				str.append(escritor + ": " +new String(ci.doFinal(msgCifrada)) + '\n');
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			received = (int) in.readObject();
		}
		
	
		System.out.println("----- Mensagens do grupo " + groupID + " -----\n");
		System.out.println(str.toString());
		System.out.println();
	}

	private static void printErrorMessage(int response) {
		if (response == -1) {
			System.out.println();
			System.err.println("-> O utilizador inserido não está correto!\n");
		} else if (response == -2) {
			System.out.println();
			System.err.println("-> Você nao é o dono do grupo!\n");
		} else if (response == -3) {
			System.out.println();
			System.err.println("-> Este grupo não existe!\n");
		} else if (response == -4) {
			System.out.println();
			System.err.println("-> O utilizador não pertence a este grupo!\n");
		} else if (response == -5) {
			System.out.println();
			System.err.println("-> Nao pertence ao grupo mencionado!\n");
		} else if (response == -6) {
			System.out.println();
			System.err.println("-> Este grupo não tem histórico.\n");
		}
	}

	private static int getGroupID(Scanner sc) {
		int groupID = 0;
		boolean valid = false;
		do {
			if (sc.hasNextInt()) {
				groupID = sc.nextInt();
				valid = true;
				sc.nextLine();
			} else {
				sc.nextLine();
				System.out.println("Insira um ID válido!\n");
			}
		} while (!valid);
		return groupID;
	}

	private static void getPicture(ObjectOutputStream out, ObjectInputStream in, PrivateKey privKey, Cipher ci) {
		try {
			String nome = (String) in.readObject(); //1
	
			File photo = new File(nome);
			photo.createNewFile();
			FileOutputStream file = new FileOutputStream(photo);
						
			
			
			CipherOutputStream cipherOutput = new CipherOutputStream(file, ci);
			long fileSize = (long) in.readObject(); //2
			byte[] buffer = new byte[1024];
			for (int i = 0; i < fileSize;) {
				int len = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize - i)); //3
				if (len > 0) {
					i += len;
				}
				cipherOutput.write(buffer, 0, len);
				cipherOutput.flush();
			}
			file.close();
			cipherOutput.close();
			out.writeObject(1);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	


	private static Cipher cifraDeGrupo(byte[] groupKey, PrivateKey privKey) {
	Key groupKeys = null;
	
	Cipher c;
	try {
		
			c = Cipher.getInstance(privKey.getAlgorithm());
			c.init(Cipher.UNWRAP_MODE, privKey);
			groupKeys = c.unwrap(groupKey, "AES", Cipher.SECRET_KEY);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Cipher ci = null;
		try {
			ci = Cipher.getInstance(groupKeys.getAlgorithm());
			ci.init(Cipher.ENCRYPT_MODE, groupKeys);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ci;
	}
	
	private static Cipher decifraDeGrupo(byte[] groupKey, PrivateKey privKey) {
		Key groupKeys = null;
		
		Cipher c;
		try {
			c = Cipher.getInstance(privKey.getAlgorithm());
			c.init(Cipher.UNWRAP_MODE, privKey);
			groupKeys = c.unwrap(groupKey, "AES", Cipher.SECRET_KEY);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Cipher ci = null;
		try {
			ci = Cipher.getInstance(groupKeys.getAlgorithm());
			ci.init(Cipher.DECRYPT_MODE, groupKeys);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ci;
	}
	

}
