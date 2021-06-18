package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientInputController {
	private boolean isAMove;
	private int holeIndexPlayed;
	
	private boolean isAConfirmation;
	private String confirmationAction;
	
	private boolean isDifficultyChoice;
	private boolean isBeginnerDifficulty;
	
	private boolean isLoading;
	private int p1Granary;
	private int p2Granary;
	private int p1Score;
	private int p2Score;
	private boolean isPlayer1Turn;
	private int[] jsonBoardToLoad;
	
	private boolean isNewGame;
	
	private boolean isReconnection;
	
	private boolean isEndRoundConfirmation;
	private boolean isSurrend;
	private boolean isReset;

	private String rawJSONInput;
	
	public ClientInputController(String rawJSONInput) {
		isAConfirmation = false;
		isAMove = false;
		isDifficultyChoice = false;
		isBeginnerDifficulty = false;
		isLoading = false;
		isReconnection = false;
		this.isReset = false;
		this.isPlayer1Turn = true;
		this.isNewGame = false;
		this.isEndRoundConfirmation = false;
		this.rawJSONInput = rawJSONInput;
		this.parseRawJsonInput();
	}

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
			JsonObject j = jsonObject.get("board").getAsJsonObject();
			this.p1Granary = j.get("playerOneGranaryCount").getAsInt();
			this.p2Granary = j.get("playerTwoGranaryCount").getAsInt();
			int playNumber = j.get("playerTurn").getAsInt();
			if (playNumber != 1) {
				this.isPlayer1Turn = false;
			}
			JsonArray jArr = j.get("seeds").getAsJsonArray();
			this.jsonBoardToLoad = new int[jArr.size()];
			for (int i = 0; i < jArr.size(); i++) {
				this.jsonBoardToLoad[i] = jArr.get(i).getAsInt();
			}
			this.p1Score = j.get("playerOneScore").getAsInt();
			this.p2Score = j.get("playerTwoScore").getAsInt();
			if (j.get("difficulty").getAsString().equals("easy")) {
				this.isBeginnerDifficulty = true;
			}
		}
		
		if (type.equals("new")) {
			this.isNewGame = true;
		}
		
		if (type.equals("reconnection")) {
			this.isReconnection = true;
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
	
	public boolean isReconnection() {
		return isReconnection;
	}

	public void setReconnection(boolean isReconnection) {
		this.isReconnection = isReconnection;
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
}
