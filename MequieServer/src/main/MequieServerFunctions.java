package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import catalogs.GroupCatalog;
import catalogs.UserCatalog;
import exceptions.IllegalGroupOperationException;
import exceptions.NoSuchGroupException;
import exceptions.NoSuchUserException;
import utilities.Group;
import utilities.Message;
import utilities.User;

public class MequieServerFunctions {

	private static GroupCatalog groupCatalog = GroupCatalog.getInstance();
	private static UserCatalog userCatalog = UserCatalog.getCatalog();

	private static Group getGroup(int groupID) throws NoSuchGroupException {
		if (!groupCatalog.containsGroup(groupID))
			throw new NoSuchGroupException("-> Erro: Este grupo não existe.\n");
		return groupCatalog.getGroup(groupID);
	}

	private static User getUser(String username) throws NoSuchUserException {
		if (!userCatalog.containsUser(username))
			throw new NoSuchUserException("-> Erro: Este utilizador não existe.\n");
		return userCatalog.getUser(username);
	}

	protected static void createGroup(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws IOException {
		try {
			int groupID;
			do {
				groupID = (Integer) inStream.readObject(); //1
				if (groupID == 0 || groupCatalog.containsGroup(groupID))
					outStream.writeObject(false); //2
			} while (groupCatalog.containsGroup(groupID));
			outStream.writeObject(true); //3
			//Recebe a chave wrapped com a chave publica do dono do grupo.
			int keySize = (int)inStream.readObject(); //4
			byte[] adminKey = new byte[keySize];
			int x = inStream.read(adminKey,0,keySize); //5
			groupCatalog.addGroup(groupID, currentUser);
			System.out.println("-> Foi criado o grupo " + groupID + ". O administrador é o utilizador " + currentUser.getUsername() + ".\n");
			groupCatalog.setKey(groupID,adminKey);

		} catch (ClassNotFoundException e) { 
			e.printStackTrace();
		}
	}

	protected static void addUser(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws IOException, NoSuchUserException, NoSuchGroupException, IllegalGroupOperationException {
		try {
			String username = (String) inStream.readObject(); //1
			User user = getUser(username);
			outStream.writeObject(2); //2
			int groupID = (int) inStream.readObject(); //3

			if(groupID == 0) {
				throw new IllegalGroupOperationException("-> Erro: Não são permitidas ações sobre o grupo geral.\n");
			}

			Group group = getGroup(groupID);
			if (!group.isOwner(currentUser)) {
				throw new IllegalGroupOperationException("-> Erro: O utilizador " + currentUser +" não é o administrador do grupo.\n");
			}


			group.addUser(user);
			outStream.writeObject(1); //4

			changeToNewKeys(group, inStream, outStream);

			System.out.println("-> O utilizador " + username + " foi adicionado ao grupo " + groupID + ".\n");
			outStream.writeObject(1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	protected static void removeUser(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws IOException, ClassNotFoundException, NoSuchUserException, NoSuchGroupException,IllegalGroupOperationException {
		String username = (String) inStream.readObject(); //1
		User user = getUser(username);
		outStream.writeObject(2); //2

		int groupID = (int) inStream.readObject(); //3
		Group group = getGroup(groupID);

		if (groupID == 0 ||!group.isOwner(currentUser)) {
			throw new IllegalGroupOperationException("-> Erro: O utilizador " + currentUser +" não é administrador do grupo.\n");
		}

		group.removeUser(user);

		changeToNewKeys(group, inStream, outStream);

		System.out.println("-> O utilizador " + username + " foi removido do grupo " + groupID + "\n");
		outStream.writeObject(1);
	}


	private static void changeToNewKeys (Group group, ObjectInputStream inStream, ObjectOutputStream outStream) throws ClassNotFoundException {
		List<String> nameUsers = group.userFromGroup();

		try {
			outStream.writeObject(nameUsers);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} //5



		List<byte[]> userKeys = new ArrayList<>();
		for(int i = 0; i < group.numMembers();i++) {
			try {
				int keySize = (int)inStream.readObject();
				byte[] userKey= new byte[keySize];
				inStream.read(userKey);
				userKeys.add(userKey); //6
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //4

		}


		group.setAdminKey(userKeys.get(0));
		group.setKey(userKeys);

	}

	protected static void ginfo(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws IOException, ClassNotFoundException, NoSuchGroupException, IllegalGroupOperationException {
		outStream.writeObject(2);
		Group group = getGroup(inStream);
		outStream.writeObject(1);

		String info = group.info(currentUser);
		outStream.flush();
		outStream.writeObject(info);
	}

	protected static void uinfo(ObjectOutputStream outStream, User currentUser) throws IOException {
		StringBuilder userOwns = new StringBuilder();
		userOwns.append("-> Administrador dos grupos:");

		StringBuilder userBelongs = new StringBuilder();
		userBelongs.append("-> Membro dos grupos: ");

		for (Integer i : groupCatalog.getGroups()) {
			if(i == 0) continue;
			Group g = groupCatalog.getGroup(i);
			if (g.isOwner(currentUser)) {
				userOwns.append(i + ", ");
			}
			if (g.isGroupMember(currentUser)) {
				userBelongs.append(i + ", ");
			}
		}
		userOwns.deleteCharAt(userOwns.length() - 1);
		userOwns.setCharAt(userOwns.length()-1, '.');
		userBelongs.deleteCharAt(userBelongs.length() - 1);
		userBelongs.setCharAt(userBelongs.length()-1, '.');
		String info = userOwns.toString() + "\n\n" + userBelongs.toString() +"\n";
		outStream.flush();
		outStream.writeObject(info);
	}

	protected static void msg(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws IOException, ClassNotFoundException, NoSuchGroupException, IllegalGroupOperationException {
		Group group = getGroup(inStream); //1
		if (!group.isGroupMember(currentUser)) {
			throw new NoSuchElementException();
		}
		outStream.writeObject(2); //2

		//Enviar a ultima chave cifrada do grupo
		byte[] key = group.getChaveCifrada(currentUser);

		outStream.writeObject(key); //3 -> Chave mais recente do Utilizador.
		int cifraSize = (int)inStream.readObject();
		byte[] cifra = new byte[cifraSize];
		inStream.read(cifra,0,cifraSize);
		group.addMessage(cifra, currentUser);
		System.out.println("-> Mensagem Recebida!");
	}

	protected static void photo(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws NoSuchGroupException, IllegalGroupOperationException {
		try {
			int groupID = (Integer) inStream.readObject(); //1
			if (groupID == 0 || !groupCatalog.containsGroup(groupID)) {
				outStream.writeObject(-3); //2
				throw new NoSuchGroupException("Grupo não existe.\n");
			}
			Group g = groupCatalog.getGroup(groupID);
			if (!g.isGroupMember(currentUser)) {
				outStream.writeObject(-5);  //2
				throw new IllegalGroupOperationException("Não é membro deste grupo.\n");
			}
			outStream.writeObject(2); //2

			//Enviar a ultima chave cifrada do grupo
			byte[] key = g.getChaveCifrada(currentUser);

			outStream.writeObject(key); //3 -> Chave mais recente do Utilizador.


			String path = g.getPhotoPath();
			String photoName = (String) inStream.readObject();  //4

			//			String extention = photoName.split("\\.")[1];
			//			String cifrePhoto = photoName.split("\\.")[0] + ".cif";

			File f = new File(path + File.separator + photoName);
			f.createNewFile();
			FileOutputStream file = new FileOutputStream(f);


			long fileSize = (long) inStream.readObject(); //5
			byte[] buffer = new byte[1024];
			for (int i = 0; i < fileSize;) {
				int len = inStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - i));
				if (len > 0) {
					i += len;
				}
				file.write(buffer, 0, len);
				file.flush();
			}
			file.close();

			Message msg = new Message(photoName, currentUser,g.getLatestKeyId());
			msg.hasRead(currentUser);
			g.addMessage(msg);
			outStream.writeObject(1);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	protected static void collect(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws IOException {
		ArrayList<ArrayList<Message>> messages = new ArrayList<>();
		ArrayList<Group> groups = currentUser.getUserGroups();
		for (Group group : groups) {
			outStream.writeObject(5);
			if(group.getID() == 0) continue;

			ArrayList<Message> groupMessages = group.unreadMessages(currentUser);
			outStream.writeObject(group.getID());

			for (Message message : groupMessages) {
				outStream.writeObject(true);
				byte[] msg = group.getChaveCifrada(message,currentUser);
				outStream.writeObject(msg.length); //Passa a Chave que decifra a msg.
				outStream.write(msg,0,msg.length);
				if(message.hasExtention()) {
					outStream.writeObject(1000);
					sendPhotoToClient(outStream, inStream, message, group, currentUser);
					if((message.howManyRead()+1) == group.numMembers()) {
						File f = new File(group.getPhotoPath() + File.separator + message.fileName());
						if(f.delete()) {
							System.out.println("-> Ficheiro apagado com sucesso.");
							group.deleteMessage(message);
						}
						else
							System.out.println("-> Ficheiro não apagado.");
					}
				}

				else {
					outStream.writeObject(2000); //Indicar que eh uma mensagem de texto.
					int size = message.mensagemCifrada().length;
					outStream.writeObject(size);
					outStream.write(message.mensagemCifrada(),0,message.mensagemCifrada().length);
					outStream.writeObject(message.escritor());
				}
			}
			outStream.writeObject(false);
			if (!groupMessages.isEmpty()) {
				messages.add(groupMessages);
				group.readMessages(currentUser);            //TODO -> para ele ficar marcado como leu tudo
			}
		}
		outStream.writeObject(3);
		if (messages.isEmpty()) {
			outStream.writeObject("Nada para ler!\n\n");
		}
	}

	protected static void history(ObjectOutputStream outStream, ObjectInputStream inStream, User currentUser) throws IOException, ClassNotFoundException, NoSuchGroupException, IllegalGroupOperationException {
		Group group = getGroup(inStream);
		if (!group.isGroupMember(currentUser)) {
			outStream.writeObject(-6);
			throw new NoSuchElementException();
		}
		outStream.writeObject(1);
		List<Message> history = group.history();
		
		for(Message message : history) {
			outStream.writeObject(5);
			byte[] chave = group.getChaveCifrada(message,currentUser);
			if(chave == null) continue;
			outStream.writeObject(chave.length);
			outStream.write(chave,0,chave.length);
			byte[] msgCifrada = message.mensagemCifrada();
			outStream.writeObject(msgCifrada.length);
			outStream.write(msgCifrada,0,msgCifrada.length);
			outStream.writeObject(message.escritor());
		}
		
		
		outStream.writeObject(3);
	}

	private static Group getGroup(ObjectInputStream inStream) throws NoSuchGroupException, ClassNotFoundException, IOException, IllegalGroupOperationException {
		int groupID = (Integer) inStream.readObject();
		if(groupID == 0)
			throw new IllegalGroupOperationException("Erro: Não são permitidas ações sobre o grupo geral.\n");
		if (!groupCatalog.containsGroup(groupID))
			throw new NoSuchGroupException("Erro: Este grupo não existe.\n");
		Group group = groupCatalog.getGroup(groupID);
		return group;
	}



	//Checkar later.
	private static void sendPhotoToClient(ObjectOutputStream outStream, ObjectInputStream inStream, Message mensagem, Group grupo, User currentUser) {
		try {
			File foto = new File(grupo.getPhotoPath() + File.separator + mensagem.fileName());
			outStream.writeObject(mensagem.fileName()); // 1 

			long fileSize = foto.length();
			byte[] buffer = new byte[1024];

			FileInputStream fileInput = new FileInputStream(foto);
			outStream.writeObject(fileSize); //2
			for (int i = 0; i < fileSize;) {
				int toWrite = fileInput.read(buffer, 0, (int) Math.min(buffer.length, fileSize - i));
				if (toWrite > 0) {
					i += toWrite;
				}
				outStream.write(buffer, 0, toWrite); //3
				outStream.flush();
			}
			fileInput.close();
			int y = (int) inStream.readObject();
			if(y != 1)
				throw new IOException();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
