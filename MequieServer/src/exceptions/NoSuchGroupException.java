package exceptions;

public class NoSuchGroupException extends Exception{
	
	private static final long serialVersionUID = 1L;

	public NoSuchGroupException(String message) {
		super(message);
	}
	
}
