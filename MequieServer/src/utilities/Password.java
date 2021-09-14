package utilities;

import java.util.Base64;

/**
 * Password class
 * @author Afonso Goncalves 49486 && Andre Cruz 51067 && Francisco Martins 51073
 *
 */
public class Password {
	
	/**
	 * Password salt
	 */
	private byte[] salt;
	
	/**
	 * Hashed password
	 */
	private byte[] hash;
	
	/**
	 * Creates a password
	 * @param password - User's password
	 * @requires password != null
	 */
	public Password(String password) {
		salt = HashPasswords.generateSalt();
		hash = HashPasswords.hash(password.toCharArray(), salt);
	}
	
	/**
	 * Creates a password
	 * @param salt - Password salt
	 * @param hash - Hashed password
	 * @requires salt != null && hash != null
	 */
	public Password(String salt, String hash) {
		this.salt = Base64.getUrlDecoder().decode(salt);
		this.hash = Base64.getUrlDecoder().decode(hash);
	}
	
	/**
	 * Password salt
	 * @return password salt
	 */
	public String salt() {
		return Base64.getUrlEncoder().encodeToString(salt);
	}
	
	/**
	 * Hashed password
	 * @return hashed password
	 */
	public String hash() {
		return Base64.getUrlEncoder().encodeToString(hash);
	}
	
	/**
	 * Password guess is correct
	 * @param userGuess
	 * @return true if the guess is correct, false otherwise
	 * @requires password != null
	 */
	public boolean authenticate(String userGuess) {
		return HashPasswords.isExpectedPassword(userGuess.toCharArray(), salt, hash);
	}
}
