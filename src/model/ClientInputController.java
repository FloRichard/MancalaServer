package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class used to catch clients requests and making them understandable for the board. 
 * Clients send raw JSON, the controller parses it and indicates the type of the request.
 * Each type of request has specific associated data. 
 * The templates for json inputs is the following :
 * 		{"type":"type of the request", "data"...}
 * @author Florian RICHARD
 * @author Julien MONTEIL
 *
 */
public class ClientInputController {
	/**
	 * Indicates that the player has played.
	 */
	private boolean isAMove;
	private int holeIndexPlayed;
	
	/**
	 * Indicates that the player has confirmed his move.
	 */
	private boolean isAConfirmation;
	private String confirmationAction;
	
	/**
	 * Indicates that the player has set the difficulty of the game.
	 */
	private boolean isDifficultyChoice;
	private boolean isBeginnerDifficulty;
	
	/**
	 * Indicates that the player wants to load a game.
	 */
	private boolean isLoading;
	private int p1Granary;
	private int p2Granary;
	private int p1Score;
	private int p2Score;
	private boolean isPlayer1Turn;
	private int[] jsonBoardToLoad;
	
	/**
	 * Indicates that the player wants to make a new game.
	 */
	private boolean isNewGame;

	private boolean isPlayerName;
	private String playerName;

	/**
	 * Indicates that the player is ready to play the next round.
	 */
	private boolean isEndRoundConfirmation;
	
	/**
	 * Indicates that the player is surrending this round.
	 */
	private boolean isSurrend;
	
	/**
	 * Indicates that the player will wait for the other player.
	 */
	private boolean isReset;

	/**
	 * The JSON sent by the player.
	 */
	private String rawJSONInput;
	
	public ClientInputController(String rawJSONInput) {
		isAConfirmation = false;
		isAMove = false;
		isDifficultyChoice = false;
		isBeginnerDifficulty = false;
		this.isPlayerName = false;
		isLoading = false;
		this.isReset = false;
		this.isPlayer1Turn = true;
		this.isNewGame = false;
		this.isEndRoundConfirmation = false;
		this.rawJSONInput = rawJSONInput;
		this.parseRawJsonInput();
	}

	/**
	 * Parse the JSON inputs and set the type of the request and its data
	 * 
	 */
	public void parseRawJsonInput() {
		@SuppressWarnings("deprecation")
		JsonObject jsonObject = new JsonParser().parse(rawJSONInput).getAsJsonObject();
		String type = jsonObject.get("type").getAsString();
		if (type.equals("move")) {
			this.isAMove = true;
			this.holeIndexPlayed = jsonObject.get("index").getAsInt();
		}
		
		if (type.equals("confirmation")) {
			this.isAConfirmation = true;
			this.confirmationAction =  jsonObject.get("action").getAsString();
		}
		
		if (type.equals("difficulty")) {
			this.isDifficultyChoice = true;
			if (jsonObject.get("value").getAsString().equals("easy")) {
				this.isBeginnerDifficulty = true;
			}
		}
		
		if (type.equals("load")) {
			this.isLoading = true;
			this.p1Granary = jsonObject.get("playerOneGranaryCount").getAsInt();
			this.p2Granary = jsonObject.get("playerTwoGranaryCount").getAsInt();
			int playNumber = jsonObject.get("playerNumber").getAsInt();
			if (playNumber != 1) {
				this.isPlayer1Turn = false;
			}
			JsonArray jArr = jsonObject.get("seeds").getAsJsonArray();
			this.jsonBoardToLoad = new int[jArr.size()];
			for (int i = 0; i < jArr.size(); i++) {
				this.jsonBoardToLoad[i] = jArr.get(i).getAsInt();
			}
			this.p1Score = jsonObject.get("playerOneScore").getAsInt();
			this.p2Score = jsonObject.get("playerTwoScore").getAsInt();
			if (jsonObject.get("difficulty").getAsString().equals("easy")) {
				this.isBeginnerDifficulty = true;
			}
		}
		
		if (type.equals("new")) {
			this.isNewGame = true;
		}
		
		if (type.equals("name")) {
			this.isPlayerName = true;
			this.playerName = jsonObject.get("value").getAsString();
		}
		
		if (type.equals("endRoundConfirmation")) {
			this.isEndRoundConfirmation = true;
		}
		
		if (type.equals("surrend")) {
			this.isSurrend = true;
		}
		
		if (type.equals("reset")) {
			this.isReset = true;
		}
	}

	public String getConfirmationAction() {
		return confirmationAction;
	}

	public void setConfirmationType(String confirmationType) {
		this.confirmationAction = confirmationType;
	}

	public boolean isDifficultyChoice() {
		return isDifficultyChoice;
	}

	public boolean isBeginnerDifficulty() {
		return isBeginnerDifficulty;
	}


	public boolean isAMove() {
		return isAMove;
	}

	public boolean isAConfirmation() {
		return isAConfirmation;
	}
	
	public int getHoleIndexPlayed() {
		return holeIndexPlayed;
	}

	public void setHoleIndexPlayed(int holeIndexPlayed) {
		this.holeIndexPlayed = holeIndexPlayed;
	}
	
	public int[] getJsonBoardToLoad() {
		return jsonBoardToLoad;
	}
	
	public boolean isLoading() {
		return isLoading;
	}
	
	public int getP1Granary() {
		return p1Granary;
	}

	public void setP1Granary(int p1Granary) {
		this.p1Granary = p1Granary;
	}

	public int getP2Granary() {
		return p2Granary;
	}

	public void setP2Granary(int p2Granary) {
		this.p2Granary = p2Granary;
	}

	public boolean isPlayer1Turn() {
		return isPlayer1Turn;
	}

	public void setPlayer1Turn(boolean isPlayer1Turn) {
		this.isPlayer1Turn = isPlayer1Turn;
	}
	
	public int getP1Score() {
		return p1Score;
	}

	public void setP1Score(int p1Score) {
		this.p1Score = p1Score;
	}

	public int getP2Score() {
		return p2Score;
	}

	public void setP2Score(int p2Score) {
		this.p2Score = p2Score;
	}
	

	public boolean isNewGame() {
		return isNewGame;
	}

	public void setNewGame(boolean isNewGame) {
		this.isNewGame = isNewGame;
	}
	
	public boolean isEndRoundConfirmation() {
		return isEndRoundConfirmation;
	}

	public void setEndRoundConfirmation(boolean isEndRoundConfirmation) {
		this.isEndRoundConfirmation = isEndRoundConfirmation;
	}
	
	public boolean isSurrend() {
		return isSurrend;
	}

	public void setSurrend(boolean isSurrend) {
		this.isSurrend = isSurrend;
	}
	
	public boolean isReset() {
		return isReset;
	}

	public void setReset(boolean isReset) {
		this.isReset = isReset;
	}
	
	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}
	public boolean isPlayerName() {
		return isPlayerName;
	}

	public void setPlayerName(boolean isPlayerName) {
		this.isPlayerName = isPlayerName;
	}
}
