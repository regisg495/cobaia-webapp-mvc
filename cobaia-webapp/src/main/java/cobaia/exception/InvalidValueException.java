package cobaia.exception;

public class InvalidValueException extends IllegalArgumentException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public InvalidValueException(String msg) {
		super(msg);
	}
	public String toString() {
		return "Value no Matches";
	}

}
