package model;

public class NotYourTurnException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NotYourTurnException(String errorMessage) {
		super(errorMessage);
	}
}
