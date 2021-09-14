package utilities;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Generates secure passwords
 * @author Afonso Goncalves 49486 && Andre Cruz 51067 && Francisco Martins 51073
 *
 */
public class HashPasswords {
	
	/**
	 * Generates an array of random bytes
	 * @return array of random bytes
	 */
	public static byte[] generateSalt() {
		Random r = new SecureRandom();
		byte[] salt = new byte[16];
		r.nextBytes(salt);
		return salt;
	}

	/**
	 * Hashes a password with a random number of bytes
	 * @param password - Password
	 * @param salt - Random number of bytes
	 * @return hashed password
	 * @requires password != null && salt != null
	 */
	public static byte[] hash(char[] password, byte[] salt) {
		byte[] saltedHash = null;
		PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 256);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			saltedHash = skf.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		} finally {
			spec.clearPassword();
		}
		return saltedHash;
	}
	
	/**
	 * Password guess is correct
	 * @param password - Password guess
	 * @param salt - Password salt
	 * @param expectedHash - Expected hashed password
	 * @return true if the guess is correct, false otherwise
	 * @requires password != null && salt != null && expectedHash != null
	 */
	public static boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) {
	    byte[] passwordHash = hash(password, salt);
	    if (passwordHash.length != expectedHash.length) {
	    	return false;
	    }
	    for (int i = 0; i < passwordHash.length; i++) {
	      if (passwordHash[i] != expectedHash[i]) {
	    	  return false;
	      }
	    }
	    return true;
	  }
}
