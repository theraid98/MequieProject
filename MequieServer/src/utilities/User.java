package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Set;

import catalogs.GroupCatalog;

/**
 * User class
 * @author Afonso Goncalves 49486 && Andre Cruz 51067 && Francisco Martins 51073
 *
 */
public class User {
	
	
	private File userFile;

	/**
	 * User's username
	 */
	private String username;

	/**
	 * User's password
	 */
	
	private String certificado;
	
	
	/**
	 * Creates a user
	 * @param username - User's username
	 * @param password - User's password
	 * @requires username != null && password != null
	 */
	
	public User(String username, Certificate certificadoName) {
		this.userFile = new File("Users"+ File.separator + username + File.separator);
		this.userFile.mkdirs();
		this.username = username;
		//this.password = password;
		try {
			byte[] buffer = certificadoName.getEncoded();
			File certificateFile = new File("PubKeys"+ File.separator + username + ".cer");
			certificateFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(certificateFile);
			//fos.write("-----BEGIN CERTIFICATE-----\n".getBytes("US-ASCII"));
			fos.write(buffer);
			//fos.write("-----END CERTIFICATE-----\n".getBytes("US-ASCII"));
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * User's username
	 * @return user's username
	 */
	public String getUsername() {
		return username;
	}
	
	
	public String getCertificate() {
		return "PubKeys"+ File.separator + username + ".cer"; 
	}
	
	//Testar.
	public PublicKey getPublicKey() throws CertificateException, FileNotFoundException {
		CertificateFactory cf = CertificateFactory.getInstance("X509");
		return cf.generateCertificate(new FileInputStream(certificado)).getPublicKey();
	}
	
	
	
	
	

	/**
	 * User's password
	 * @return user's password
	 */
//	public Password getPassword() {
//		return password;
//	}

	/**
	 * See all the Groups where User belongs
	 * @return - ArrayList with the Groups of the User
	 */
	public ArrayList<Group> getUserGroups(){
			ArrayList<Group> userGroups = new ArrayList<>();
			Set<Integer> grupos = GroupCatalog.getInstance().getGroups();
			for(int i: grupos) {
				Group g = GroupCatalog.getInstance().getGroup(i);
				if(g.isGroupMember(this))
					userGroups.add(g);
					
			}
			return userGroups;
	}

	/**
	 * Password guess is correct
	 * @param password - Password guess
	 * @return true if the guess is correct, false otherwise
	 * @requires password != null
	 */
//	public boolean authenticate(String password) {
//		return this.password.authenticate(password);
//	}
}
