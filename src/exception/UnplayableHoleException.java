package exception;

public class UnplayableHoleException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static String NotYourAreaErr = "{\"type\":\"error\",\"value\":\"error.notYourArea\"}";
	public static String EmptyHoleErr = "{\"type\":\"error\",\"value\":\"error.emptyHole\"}";
	public static String MoveIsNotFeedingErr = "{\"type\":\"error\",\"value\":\"error.notFeedingMove\"}";
	public static String MoveIsStarvingErr = "{\"type\":\"error\",\"value\":\"error.isStarving\"}";
	
	
	public UnplayableHoleException(String errorMessage) {
		super(errorMessage);
	}
}
