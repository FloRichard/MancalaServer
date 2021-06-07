package model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientInputController {
	private boolean isAMove;
	private int holeIndexPlayed;
	private String confirmationAction;
	private boolean isAConfirmation;
	private boolean isDifficultyChoice;
	private boolean isBeginnerDifficulty;
	private String rawJSONInput;
	
	public ClientInputController(String rawJSONInput) {
		isAConfirmation = false;
		isAMove = false;
		isDifficultyChoice = false;
		isBeginnerDifficulty = false;
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

	
	
	
}
