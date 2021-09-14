package utilities;

import java.util.ArrayList;

/**
 * Message class
 * @author Afonso Goncalves 49486 && Andre Cruz 51067 && Francisco Martins 51073
 *
 */
public class Message {

	/**
	 * Message
	 */
	//	private String message;

	/**
	 * Users who read the message
	 */
	private ArrayList<User> usersWhoRead;


	private byte[] mensagemCifrada;

	int id;

	/*
	 * If the Message is from a file
	 */
	private String fileName;

	/**
	 * Creates a message 
	 * @param message - Message
	 * @param user - User who sent the message
	 * @requires message != null && user != null
	 */
	public Message(byte[] photoName, User user, int id) {
		this.mensagemCifrada = photoName;
		usersWhoRead = new ArrayList<>();
		this.id = id;
		usersWhoRead.add(user);
	}

	/**
	 * Creates a message to files
	 * @param fileName
	 * @param user - User who sent the message
	 */
	public Message(String fileName, User user, int id) {
		this.id = id;
		this.fileName = fileName;
		usersWhoRead = new ArrayList<>();
		usersWhoRead.add(user);
		mensagemCifrada = fileName.getBytes();
	}
	
	/**
	 * Devolve o Id da Chave utilizada para cifrar a Mensagem.
	 * @return 
	 */
	public int getCipherId() {
		return id;
	}
	
	public String escritor() {
		return this.usersWhoRead.get(0).getUsername();
	}

	/**
	 * Message
	 * @return message
	 */
	//	public String getMessage() {
	//		return message;
	//	}

	/**
	 * Nome do ficheiro com extensao
	 * @return
	 */

	public String fileName() {
		return fileName;
	}


	public byte[] mensagemCifrada() {
		return mensagemCifrada;
	}


	/**
	 * Has user read the message
	 * @param user - User
	 * @return true if the user read the message, false otherwise
	 * @requires user != null
	 */
	public boolean userRead(User user) {
		return usersWhoRead.contains(user);
	}

	/**
	 * How many users read the message
	 * @return number of users who read the message
	 */
	public int howManyRead() {
		return usersWhoRead.size();
	}

	/**
	 * User read the message
	 * @param user - User who read the message
	 * @requires user != null
	 */
	public void hasRead(User user) {
		if(!userRead(user))
			usersWhoRead.add(user);
	}


	/**
	 * Da toda a informacao da Mensagem no seguinte Padrao: (*id*:user,user,user,etc...)
	 * em byte[] cifrado com chave simetrica.
	 * @return Informacao da mensagem em byte[].
	 */
	public byte[] msgInfo() {
		StringBuilder str = new StringBuilder();
//		if(this.hasExtention())
//			str.append(extention + "//");
		str.append(this.id);
		str.append(":");
		for(User u: this.usersWhoRead) {
			str.append(u.getUsername());
			str.append(",");
		}
		str.deleteCharAt(str.length()-1);
		return str.toString().getBytes();
	}


//	public void setExtention (String extention) {
//		this.extention = extention;
//	}
//
	public boolean hasExtention () {
		return this.fileName != null;
	}
//
//	public String getExtention () {
//		return this.extention;
//	}


	/**
	 * Passes the Message to String
	 */
	//	public String toString() {
	//		StringBuilder str = new StringBuilder(this.message);
	//		str.append("//");
	//		for(User u: usersWhoRead) {
	//			str.append(u.getUsername() + ",");
	//		}
	//		str.deleteCharAt(str.length()-1); //Removes ','
	//		return str.toString();
	//	}
}
