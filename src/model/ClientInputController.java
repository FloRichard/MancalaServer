package model;

import org.json.*;
import org.json.simple.JSONObject;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ClientInputController {
	private boolean isAMove;
	private boolean isAConfirmation;
	private String rawJSONInput;
	public ClientInputController(String rawJSONInput) {
		this.rawJSONInput = rawJSONInput;
		this.parseRawJsonInput();
	}
	
	public String parseRawJsonInput() {
		@SuppressWarnings("deprecation")
		JsonObject jsonObject = new JsonParser().parse(rawJSONInput).getAsJsonObject();
		String type = jsonObject.get("type").getAsString();
		if (type.equals("move")) {
			this.isAMove = true;
			return jsonObject.get("index").getAsString();
		}
		
		if (type.equals("confirmation")) {
			this.isAConfirmation = true;
			return "";
		}
		return "";
	}
	
	
	
}
