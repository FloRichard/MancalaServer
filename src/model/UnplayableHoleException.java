package model;

public class UnplayableHoleException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public UnplayableHoleException(String errorMessage) {
		super(errorMessage);
	}
}
