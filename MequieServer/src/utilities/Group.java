package utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import catalogs.UserCatalog;

/**
 * Group class
 * @author Afonso Goncalves 49486 && Andre Cruz 51067 && Francisco Martins 51073
 *
 */
public class Group {

	/**
	 * Group admin
	 */
	private User admin;

	/**
	 * Group ID
	 */
	private int id; // String.

	/**
	 * Group members
	 */
	private ArrayList<User> members;

	/**
	 * Group inbox
	 */
	private ArrayList<Message> inbox;

	/**
	 * Users Catalog
	 */
	private UserCatalog userCatalog;

	/**
	 * File with all the groups
	 */
	private File groupFile;

	private List<File> keyFiles;

	private File pathFile;

	private File messageFile;

	private File historyFile;

	private File photoPath;	

	/**
	 * All messages in the group
	 */
	private ArrayList<Message> messageHistory;

	//Mapa com Chaves do Grupo. Atualizada cada vez que um User eh adicionado.
	private Map<Integer, byte[]> sharedKey;

	//Mapa com a Lista de Chaves para cada User.
	private Map<String, List<byte[]>> groupKey;

	/**
	 * Creates a group
	 * @param id - Group ID
	 * @param owner - User who creates the group
	 * @requires owner != null
	 * @throws IOException
	 */
	public Group(int id, User owner) throws IOException {
		this.id = id;
		admin = owner;
		//Conversao de Byte[] para SecretKey.
		sharedKey = new HashMap<>();
		keyFiles = new ArrayList<>();
		groupKey = new HashMap<>();
		newGroup();
	}

	public Group(int i) { //For general group only.
		this.id = i;
		try {
			newGroup();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public int getLatestKeyId() {
		return sharedKey.size()-1;
	}


	public void setAdminKey(byte[] key) {
		sharedKey.put(sharedKey.size(), key);
		List<byte[]> keys = groupKey.get(admin.getUsername());
		keys.add(key);
		groupKey.replace(admin.getUsername(), keys);
	}


	/**
	 * Loads Keys from Files.
	 * @throws IOException
	 */
	private void loadKeys() throws IOException {
		for(User u: members) {
			System.out.println("-> Leitura de Chaves, Membro:" + u.getUsername()); 
			File f = new File(this.pathFile.getPath() + File.separator + "Keys" + File.separator + u.getUsername());
			FileInputStream in = new FileInputStream(f);
			List<byte[]> userKeys = new ArrayList<>();
			while(in.available() > 0) {
				//LEITURA DA INFO:
				int infoSize = in.read();
				byte[] info = new byte[infoSize];
				int lidos = in.read(info,0,infoSize);

				//LEITURA DA CHAVE:
				byte[] key = new byte[256];
				in.read(key,0,256);
				String s = new String(info);
				int id = Integer.parseInt(s.split(":")[1]);
				System.out.println("-> Chave: " + id + " do " + u.getUsername() + " lida.");
				if(u.equals(admin))
					sharedKey.put(id, key);
				while(userKeys.size() < id)
					userKeys.add(null);
				userKeys.add(key);
			}
			groupKey.put(u.getUsername(), userKeys);
			this.keyFiles.add(f);
			in.close();
		}
		System.out.println(this.keyFiles.size());
	}



	public void setKey(List<byte[]> keys) {
		for(int i = 1; i < members.size(); i++) {
			List<byte[]> antigaLista = groupKey.get(members.get(i).getUsername()); //dar replace da lista do user na groupKey. para ir sendo adicionado
			while(antigaLista.size() != sharedKey.size()-1)
				antigaLista.add(null);
			antigaLista.add(keys.get(i));
			groupKey.replace(members.get(i).getUsername(), antigaLista);
		}
		int count = 0;
		for(File f: keyFiles) {
			try {
				FileOutputStream out = new FileOutputStream(f,true);
				byte[] info;
				System.out.println("-> Escrevi a Chave " + (sharedKey.size()-1) + " do utilizador " + members.get(keyFiles.indexOf(f)).getUsername());
				info = ("id:" + (sharedKey.size()-1)).getBytes();
				out.write(new Integer(info.length).byteValue());
				out.write(info);
				out.write(keys.get(count));
				count++;
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private void newGroup() throws IOException {
		pathFile = new File("groups" + File.separator +"group" + id);
		userCatalog = UserCatalog.getCatalog();
		//ArrayLists 
		members = new ArrayList<>();
		inbox = new ArrayList<>();
		messageHistory = new ArrayList<>();

		//Files
		groupFile = new File(pathFile.getPath() + File.separator + "Users.txt");
		historyFile = new File(pathFile.getPath() + File.separator + "History.txt");
		messageFile = new File(pathFile.getPath() + File.separator + "Messages.txt");
		photoPath = new File(pathFile.getPath() + File.separator + "Photos");
		File keyPath = new File(pathFile.getPath() + File.separator + "Keys" + File.separator);
		//Mkdirs -> To create the Group folders.
		pathFile.mkdirs();
		pathFile.setWritable(true, false);
		photoPath.mkdirs();
		photoPath.setWritable(true, false);
		keyPath.mkdirs();
		keyPath.setWritable(true, false);

		loadGroupFile();
		goToHistory(); //Check where it is used.
	}



	/**
	 * Reads the data from the group file
	 */
	private void loadGroupFile() {
		try {
			if (groupFile.createNewFile()) {
				// It is a new Group, and no Loading is required.
				System.out.println("-> Ficheiro de Users do Grupo criado.");
				if(admin != null)
					this.addUser(this.admin);
				if(messageFile.createNewFile()) {
					System.out.println("-> Ficheiro de Mensagem criado.");
				}
				if(historyFile.createNewFile()) {
					System.out.println("-> Ficheiro do Historico criado.");
					return;
				}
			} else { // Not a new group, Load is required.
				Scanner scan = new Scanner(this.groupFile);
				while(scan.hasNextLine()) {
					String user = scan.nextLine();
					this.members.add((UserCatalog.getCatalog().getUser(user)));
				}
				scan.close();
				loadHistory();
				loadMessages();
				System.out.println("-> Users de group" + this.id + " carregados. \n");
				loadKeys();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}



	public List<String> userFromGroup(){
		List<String> names = new ArrayList<>();
		for(int i = 0; i < numMembers(); i++) {
			String name = this.members.get(i).getUsername();
			names.add(name);
		}
		return names;
	}

	public int numMembers() { 
		return this.members.size();
	}

	/**
	 * Reads the Messages from the message file
	 */
	private void loadMessages() {
		this.readFile(this.messageFile);
	}


	private void loadHistory() {
		this.readFile(this.historyFile);
	}

	private void readFile(File f){
		try {
			FileInputStream in = new FileInputStream(f);
			while(in.available() > 0) {
				int msgSize = in.read(); //Leitura do tamanho da Mensagem.
				int dataSize = in.read(); //Leitura do Tamanho da Info da mensagem.
				byte[] msg = new byte[msgSize];
				in.read(msg,0,msgSize); //Leitura da Mensagem.
				byte[] data = new byte[dataSize];
				in.read(data,0,dataSize); //Leitura da Info.
				String dataObj = new String(data);
				int msgId = Integer.parseInt(dataObj.split(":")[0]);
				User u = userCatalog.getUser(dataObj.split(":")[1].split(",")[0]);
				Message msgObj;
				if(new String(msg).split("\\.").length > 1)
					msgObj = new Message(new String(msg),u,id);
				else
					msgObj = new Message(msg,u,id);
				String[] quemLeu = dataObj.split(":")[1].split(",");
				for(int i = 1; i < quemLeu.length;i++) {
					User toUse = userCatalog.getUser(quemLeu[i]);
					msgObj.hasRead(toUse);
				}
				if(f.getPath().equals(this.messageFile.getPath()))
					this.inbox.add(msgObj);
				else
					this.messageHistory.add(msgObj);
			}
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}


	/**
	 * ID of the group
	 * @return ID of the group
	 */
	public int getID() {
		return this.id;
	}

	/**
	 * Add a user to the group
	 * @param user - User to add
	 * @requires u != null
	 */
	public void addUser(User user) {
		if (members.contains(user)) {
			return;
		}
		members.add(user);
		addUser(user.getUsername());
	}

	/**
	 * Private method to Add a User to the File Users
	 * @param user - User to add
	 */
	private void addUser(String user) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(groupFile,true));
			out.write(user);
			out.newLine();
			out.close();
			File f = new File(this.pathFile.getPath() + File.separator + "Keys" + File.separator + user);
			f.createNewFile();
			keyFiles.add(f);
			List<byte[]> key = new ArrayList<>();
			System.out.println("-> Adicionado Utilizador: " + user);
			groupKey.put(user, key);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Remove a user from the group
	 * @param u - User to remove
	 * @requires u != null
	 */
	public void removeUser(User user) {
		if (!members.contains(user)) {
			throw new NoSuchElementException();
		}
		File f = keyFiles.get(members.indexOf(user));
		f.delete();
		keyFiles.remove(f);
		members.remove(user);
		removeUser(user.getUsername());
	}

	/**
	 * Remove a User from the Group (also from the User files)
	 * @param user - User to Remove
	 */
	private void removeUser(String user) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(groupFile));
			StringBuilder sb = new StringBuilder();
			String line;
			while((line = in.readLine()) != null) {
				if(!line.equals(user)) {
					sb.append(line+"\n");
				}
			}
			in.close();
			BufferedWriter out = new BufferedWriter(new FileWriter(groupFile));
			out.write(sb.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Verify if a user is a member of the group
	 * @param u - User to verify
	 * @return true if the user is a member of the group, false otherwise
	 * @requires u != null
	 */
	public boolean isGroupMember(User u) {
		return members.contains(u);
	}

	/**
	 * Verify if the User is the Owner of the Group
	 * @param u - User to verify
	 * @return true if the User is the Owner
	 */
	public boolean isOwner(User u) {
		return admin.equals(u);
	}

	/**
	 * Information of the Group
	 * @param current_user - User which we want to see the information
	 * @return - a String with all the information available, regarding this User
	 */
	public String info(User current_user) {
		StringBuilder sb = new StringBuilder();
		sb.append("----- Informacao do Grupo ----- \n");
		sb.append(" - Owner: " + admin.getUsername() + "\n" + " - Numero de Clientes: " + members.size() + "\n");
		if(members.size() == 1) {
			sb.append(" - Este Grupo nao possui Clientes, para alem do Dono.");
		}else {
			if (isOwner(current_user)) {

				sb.append("\n----- Clientes ----- \n");
				for (int i = 0; i < members.size(); i++) {
					sb.append("Cliente " + (i + 1) + ": " + members.get(i).getUsername() + "\n");
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Add a Message to this Group
	 * @param msg - Message to add
	 * @param u - User which is inserting the Message
	 */
	public void addMessage(byte[] cifra, User u) {
		Message mensagem = new Message(cifra, u,groupKey.get(u.getUsername()).size()-1);
		inbox.add(mensagem);
		writeMessage(mensagem,"inbox");
		goToHistory();
	}

	/**
	 * Writes the Message to the File
	 * @param msg - Message to be inserted
	 * @param file - File to insert the message (inbox or history).
	 */
	private void writeMessage(Message msg,String file) {
		File f = this.messageFile;
		if(file.equals("history"))
			f = this.historyFile;
		byte[] info = msg.msgInfo();
		try {
			FileOutputStream out = new FileOutputStream(f,true);
			out.write(new Integer(msg.mensagemCifrada().length).byteValue()); //Escreve o tamanho da mensagem.
			out.write(new Integer(info.length).byteValue());
			out.write(msg.mensagemCifrada());
			out.write(info);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public List<Message> history() {
		return messageHistory;
	}


	/**
	 * Message which the selected User didn't read
	 * @param current_user - User selected that want to see his unread Messages
	 * @return - String with the Messages unread
	 */
	public ArrayList<Message> unreadMessages(User current_user) {	
		ArrayList<Message> canRead = new ArrayList<>();
		List<byte[]> userKeys = groupKey.get(current_user.getUsername());
		for(Message m: inbox) {
			if(!m.userRead(current_user)) {
				int cryptoId = m.getCipherId(); //Id da Chave utilizada para cifrar a mensagem.
				if(userKeys.get(cryptoId) != null)
					canRead.add(m);
			}
		}
		return canRead;
	}


	/**
	 * Verify if a User read a Message
	 * @param u - User to verify
	 */
	public void readMessages(User u){
		for(Message m: inbox) {
			m.hasRead(u);
		}
		goToHistory();
		rewriteFile();
	}

	/**
	 * 
	 */
	//	private void rewriteFile() {
	//		try {
	//			BufferedWriter bf = new BufferedWriter(new FileWriter(this.historyFile,false));
	//			for(Message msg: messageHistory) {
	//				bf.write(msg.toString());
	//				bf.newLine();
	//			}
	//			bf.close();
	//			bf = new BufferedWriter(new FileWriter(this.messageFile,false));
	//			for(Message msg:inbox) {
	//				bf.write(msg.toString());
	//				bf.newLine();
	//			}
	//			bf.close();
	//		} catch (IOException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//	}


	private void rewriteFile() {
		this.messageFile.delete();
		this.historyFile.delete();
		try {
			this.messageFile.createNewFile();
			this.historyFile.createNewFile();
			for(Message m : inbox)
				writeMessage(m,"inbox");
			for(Message m: messageHistory)
				writeMessage(m,"history");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Passing all Messages read (by all) from the Group to the History
	 */
	private void goToHistory() {
		for (int i = 0; i < inbox.size(); i++) {
			if (inbox.get(i).howManyRead() == members.size()) {
				messageHistory.add(inbox.get(i));
				inbox.remove(i);
			}
		}
	}

	/**
	 * Uses the data that has been inserted in the file to create Group object.
	 * @param groupData 
	 * @return
	 */
	public static Group fromString(String groupData) {
		UserCatalog cat = UserCatalog.getCatalog();
		String[] data = groupData.split(",");
		try {
			Group g = new Group(Integer.parseInt(data[0]), cat.getUser(data[1]));
			return g;
		} catch (NumberFormatException e) {
			System.out.println("-> Id invalido.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException, could not read from file.");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Retorna a Chave mais recente do Utilizador.
	 * @param currentUser - Utilizador a ir buscar a Chave.
	 * @return Chave mais recente do currentUser.
	 */
	public byte[] getChaveCifrada(User currentUser) {
		List<byte[]> keys = groupKey.get(currentUser.getUsername());
		return keys.get(keys.size()-1);
	}


	public byte[] getChaveCifrada(Message msg, User u) {
		return this.groupKey.get(u.getUsername()).get(msg.getCipherId());		
	}



	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("group" + id + ":" + this.id + ",");
		for (User u : this.members) {
			str.append(u.getUsername() + ",");
		}
		str.deleteCharAt(str.length() - 1);
		return str.toString();
	}

	/**
	 * Get the Path from a Photo
	 * @return - String with the Path of the Photo
	 */
	public String getPhotoPath() {
		return this.photoPath.getPath();
	}

	public void addMessage(Message msg) {
		inbox.add(msg);
		System.out.println("-> Adicionada Mensagem ao Grupo!");
		writeMessage(msg,"inbox");
	}

	public List<PublicKey> getUserKeys() {
		List<PublicKey> result = new ArrayList<>();
		for(User u: members) {
			try {
				result.add(u.getPublicKey());
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	public void deleteMessage(Message message) {
		this.inbox.remove(message);
	}
}
