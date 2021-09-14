package catalogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import utilities.User;

public class UserCatalog {

	private Map<String,User> users;
	private File userFile;
	private static UserCatalog INSTANCE = new UserCatalog();

	public static UserCatalog getCatalog() {
		return INSTANCE;
	}

	private UserCatalog() {
		users = new ConcurrentHashMap<>();
		userFile = new File("users.txt");
		readFile();
	}

	/**
	 * Verify if the User exists in the Server
	 * @param username - User to be found
	 * @return true if he exists in the Server
	 */
	public boolean containsUser(String username) {
		return users.get(username) != null;  
	}

	/**
	 * Get the wanted User
	 * @param username - User wanted
	 * @return
	 */
	public User getUser(String username) {
		return users.get(username);
	}

	/**
	 * Register a User to the Server
	 * @param name - name of the User
	 * @param password - Password of the User
	 * @throws FileNotFoundException
	 */
	public void registerUser(String name, Certificate certificadoName) throws FileNotFoundException {
		User user = new User(name, certificadoName);
		users.put(name, user);
		refreshFile(user);
	}


	//----------- MUDAR COMO SE LE O FILE PARA SE VERIFICAR CERTIFICADO -------------



	/**
	 * Read from the User File
	 */ 
	private void readFile() {
		try {
			if(userFile.createNewFile()) {
				//Enters if the file requires creation.
				System.out.println("Ficheiro de Users criado.\n");
				return;
			}
			FileInputStream in = new FileInputStream(this.userFile);
			while(in.available() > 0) {
				int numBytes = in.read();
				byte[] lineBytes = new byte[numBytes];
				in.read(lineBytes,0,numBytes);
				String line = new String(lineBytes);
				String[] userData = line.split(":");
				String username = userData[0];
				String certificate = userData[1];
				FileInputStream fis = new FileInputStream("PubKeys" + File.separator + certificate);
				CertificateFactory cf  = CertificateFactory.getInstance("X.509");
				User user = new User(username,cf.generateCertificate(fis));
				users.put(username, user);
			}
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Refresh the User File it's add a new User
	 * @param user - new User added
	 * @throws FileNotFoundException
	 */
	private void refreshFile(User user) throws FileNotFoundException {
		String username = user.getUsername();
		String userCertificate = user.getUsername();
		try {
			FileOutputStream out = new FileOutputStream(this.userFile,true);
			String escreve  = username + ":" + userCertificate + ".cer";
			out.write(escreve.length());
			out.write(escreve.getBytes());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
