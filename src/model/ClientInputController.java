package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientInputController {
	private boolean isAMove;
	private int holeIndexPlayed;
	private String confirmationAction;
	private boolean isAConfirmation;
	private boolean isDifficultyChoice;
	private boolean isBeginnerDifficulty;
	private boolean isLoading;
	private int p1Granary;
	private int p2Granary;
	private boolean isPlayer1Turn;
	private int[] jsonBoardToLoad;

	private String rawJSONInput;
	
	public ClientInputController(String rawJSONInput) {
		isAConfirmation = false;
		isAMove = false;
		isDifficultyChoice = false;
		isBeginnerDifficulty = false;
		isLoading = false;
		this.isPlayer1Turn = true;
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
			if (jsonObject.get("action").getAsString().equals("easy")) {
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
	
	
}
