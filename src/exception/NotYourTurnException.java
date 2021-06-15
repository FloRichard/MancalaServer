package exception;

public class NotYourTurnException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NotYourTurnException() {
		super("{\"type\":\"error\",\"value\":\"error.notYourTurn\"}");
	}
}
