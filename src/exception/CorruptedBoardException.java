package exception;

public class CorruptedBoardException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CorruptedBoardException() {
		super("{\"type\":\"error\",\"value\":\"error.corruptedBoard\"}");
	}

}
