package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import exception.NotYourTurnException;
import exception.UnplayableHoleException;


public class Board implements Cloneable{
	private ArrayList<Hole> holes;
	private SimplePlayer playerOne;
	private SimplePlayer playerTwo;
	private boolean isBeginnerDifficulty;
	private boolean isFull;
	
	private final String ROUND = "round";
	private final String GAME = "game";

	private int numberOfRoundPlayed;

	public Board(ArrayList<Hole> holes) {
		this.holes = holes;
		this.numberOfRoundPlayed = 0;
		this.isBeginnerDifficulty = true; // default difficulty
		this.isFull = false;
	}
	
	/**
	 * Set playerOne and playerTwo of the board.
	 * The first player to connect to the server will be the playerOne.
	 * @param player the player that will be set.
	 * @return the number of the player (1 or 2)
	 */
	public int setPlayer(SimplePlayer player) {
		if (playerOne == null) {
			this.playerOne = player;
			this.playerOne.setBlocked(false);
			return 1;
		}else {
			this.playerTwo = player;
			this.playerTwo.setBlocked(true);
			this.isFull = true;
			return 2;
		}
	}
	
	/**
	 * Function that is used as a controller of clients input.
	 * It is called by each player when they want to interact with the server.
	 * @param player the player that is playing.
	 * @param input the input of the player.
	 * @param out the output stream of the player.
	 */
	public void handleATurn(SimplePlayer player, String input) {
		ClientInputController request = new ClientInputController(input);
		if (request.isNewGame()) {
			emptyBoard();
		}
		
		if (request.isLoading()) {
			System.out.println("Loading a board...");
			this.loadFromRequest(request);
			this.broadcastMsg(getBoardToJSONString(this));
			System.out.println("Board loaded !");
			return;
		}
		
		if (request.isDifficultyChoice() && request.isBeginnerDifficulty()) {
 			return;
 		}
		
		if (request.isAMove()){
 			player.setLastMove(this.clone());
 			try {
				player.playAMove(request.getHoleIndexPlayed());
			} catch (UnplayableHoleException | NotYourTurnException e) {
				player.getOutPut().println(e.getMessage());
				System.out.println(e.getMessage());
				return;
			} catch (EasyModeWin e) {
				if (handleWin(player)) {
					return;
				}
			}
 			System.out.println("lastMove "+ getBoardToJSONString(player.getLastMove()));
 			player.getOutPut().println(getBoardToJSONString(this));
 			return;
 		}
		
		if (request.isAConfirmation() && request.getConfirmationAction().equals("abort")) {
			player.setBoard(player.getLastMove().clone());
			System.out.println("Aborting the move... Returning to"+ getBoardToJSONString(player.getLastMove()));
			player.getOutPut().println(getBoardToJSONString(this));
 			return;
 		}
		
		if (player.hasWon()) {
			if(handleWin(player)) {
				return;
			}
	 	}
		
		if (this.isNullRound()) {
			player.addPointToScore();
			player.getEnemy().addPointToScore();
			
			// Add to rounds when the match is null because each player won one point.
			this.addARound();
			this.addARound();
			if (this.getNumberOfRoundPlayed() == 6) {
				System.out.println("Game is over !");
				gameOver(player);
				return;
			}else {
				emptyBoard();
			}
			this.broadcastMsg(this.getNullJSONStringOn(ROUND));
		}
		
		this.broadcastMsg(getBoardToJSONString(this));
		
		// Unlocking the enemy, locking the actual player.
		player.getEnemy().setBlocked(false);
		player.setBlocked(true);
	}
	
	/**
	 * Handle the end of a round and check if the game is over.
	 * @param player the player that is playing.
	 * @return Returns true if the game is over.
	 */
	public boolean handleWin(SimplePlayer player) {
		this.broadcastMsg("Le joueur "+player.getPlayerNumber()+" a gagné la manche");
		player.addPointToScore();
		this.addARound();
		if (this.getNumberOfRoundPlayed() == 6) {
			System.out.println("Game is over !");
			gameOver(player);
			return true;
		}
		emptyBoard();
		return false;
	}
	
	/**
	 * Load a board from the client request.
	 * It repopulates every elements of the board.
	 * @param request the request sent by the client.
	 */
	public void loadFromRequest(ClientInputController request){
		for (int i = 0; i<this.getHoles().size(); i++) {
			this.getHoles().get(i).setSeeds(request.getJsonBoardToLoad()[i]);
		}
		this.playerOne.getGranary().setSeeds(request.getP1Granary());
		this.playerTwo.getGranary().setSeeds(request.getP2Granary());
		this.getPlayerOne().setScore(request.getP1Score());
		this.getPlayerTwo().setScore(request.getP2Score());
		this.setNumberOfRoundPlayed(request.getP1Score() + request.getP2Score());
		if (request.isPlayer1Turn()) {
			this.getPlayerOne().setBlocked(false);
			this.getPlayerTwo().setBlocked(true);
		}else {
			this.getPlayerOne().setBlocked(true);
			this.getPlayerTwo().setBlocked(false);
		}
		
		if (request.isBeginnerDifficulty()) {
				this.setBeginnerDifficulty(true);
		}
	}
	
	/**
	 * Distribute seeds from the played hole.
	 * @param holeIndex the hole played by the player
	 * @return the index of the hole where the last seed goes.
	 */
	public int distribute(int holeIndex) {
		int nbSeeds = this.getHoles().get(holeIndex).getSeeds();
		this.getHoles().get(holeIndex).removeSeeds();
		
		int index = holeIndex;
		while(nbSeeds > 0) {
			index++;
			if (index >= this.getHoles().size()){
				index = index - this.getHoles().size();
			}
			
			if (index != holeIndex) {
				this.getHoles().get(index).addSeed();
				nbSeeds--;
			}
		}
		return index;
	}
	
	/**
	 * Send a message to both players.
	 * @param msg the msg to send to players
	 */
	public void broadcastMsg(String msg) {
		try {
			PrintWriter outOne = new PrintWriter(this.getPlayerOne().getSocket().getOutputStream(), true);
			PrintWriter outTwo = new PrintWriter(this.getPlayerTwo().getSocket().getOutputStream(), true);
			outOne.println(msg);
			outTwo.println(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check if the round is a null.
	 * @return true if the round is null.
	 */
	public boolean isNullRound() {
		if (this.getSeeds() < 6) {
			return this.getPlayerOne().getGranary().getSeeds() <= 24  && this.getPlayerTwo().getGranary().getSeeds() <= 24;
		}
		return false;
	}
	
	/**
	 * Handles the end of a game, when 6 round has been played.
	 * It sends appropriate messages to each player regarding if the match is null, won or lose.
	 * @param playerNumber the player that end the game by playing.
	 */
	public void gameOver(SimplePlayer player) {	
		if (player.getScore() > 3) {
			player.getOutPut().println(getWinJSONStringOn(GAME));
			player.getEnemy().getOutPut().println(getLoseJSONStringOn(GAME));
		}else if (player.getScore() == 3) {
			player.getOutPut().println(getNullJSONStringOn(GAME));
			player.getEnemy().getOutPut().println(getNullJSONStringOn(GAME));
		}else {
			player.getOutPut().println(getLoseJSONStringOn(GAME));
			player.getEnemy().getOutPut().println(getWinJSONStringOn(GAME));
		}
	}
	
	/**
	 * Get the total number of seeds in board holes(excepted granaries)
	 * @return the number of seeds in the board.
	 */
	public int getSeeds() {
		int nbSeed = 0;
		for (Hole hole : holes) {
			nbSeed += hole.getSeeds();
		}
		return nbSeed;
	}
	
	/**
	 * Empty the board by reseting holes and granaries.
	 */
	public void emptyBoard() {
		ArrayList<Hole> holes = new ArrayList<Hole>();
		for(int i =0;i<12;i++) {
			 Hole newH = new Hole(4);
			 holes.add(newH);
		}
		this.setHoles(holes);
		this.emptyGranaries();
	}
	
	/**
	 * Empty both player's granaries.
	 */
	public void emptyGranaries() {
		this.getPlayerOne().getGranary().removeSeeds();
		this.getPlayerTwo().getGranary().removeSeeds();
	}
	
	/**
	 * Creates a JSON string representation of the actual board.
	 * @param b the board to represent in JSON.
	 * @return the JSON string.
	 */
	public static String getBoardToJSONString(Board b) {
		String JSONHoles = "[";
		for (int i = 0; i< b.getHoles().size();i++) {
			JSONHoles += b.getHoles().get(i).getSeeds();
			if (i < b.getHoles().size() -1) {
				JSONHoles += ",";
			}
		}
		JSONHoles += "]";
		
		String boardJSON = "{\"seeds\":"+JSONHoles+
				",\"playerOneGranaryCount\":"+b.getPlayerOne().getGranary().getSeeds()+
				",\"playerTwoGranaryCount\":"+b.getPlayerTwo().getGranary().getSeeds()+"}";
		return boardJSON;
	}

	public String getWinJSONStringOn(String winType) {
		return "{\"type\":\"info\",\"value\":\"win\",\"on\":"+winType+"}";
	}
	
	public String getLoseJSONStringOn(String loseType) {
		return "{\"type\":\"info\",\"value\":\"lose\",\"on\":"+loseType+"}";
	}
	
	public String getNullJSONStringOn(String nullType) {
		return "{\"type\":\"info\",\"value\":\"null\",\"on\":"+nullType+"}";
	}
	
	public ArrayList<Hole> getHoles() {
		return holes;
	}
	public void setHoles(ArrayList<Hole> holes) {
		this.holes = holes;
	}
	
	public SimplePlayer getPlayerOne() {
		return playerOne;
	}
	public void setPlayerOne(SimplePlayer playerOne) {
		this.playerOne = playerOne;
	}
	
	public SimplePlayer getPlayerTwo() {
		return playerTwo;
	}
	public void setPlayerTwo(SimplePlayer playerTwo) {
		this.playerTwo = playerTwo;
	}
	
	public boolean isBeginnerDifficulty() {
		return isBeginnerDifficulty;
	}

	public void setBeginnerDifficulty(boolean isBeginnerDifficulty) {
		this.isBeginnerDifficulty = isBeginnerDifficulty;
	}
	
	public int getNumberOfRoundPlayed() {
		return numberOfRoundPlayed;
	}
	
	public void setNumberOfRoundPlayed(int nbRound) {
		this.numberOfRoundPlayed = nbRound;
	}

	public void addARound() {
		this.numberOfRoundPlayed++;
	}
	
	public boolean isFull() {
		return isFull;
	}

	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}
	
	public Board clone() {
		ArrayList<Hole> clonedHoles = new ArrayList<Hole>();
		for(int i=0; i < this.getHoles().size(); i++) {
			Hole clonedHole = this.getHoles().get(i).clone();
			clonedHoles.add(clonedHole);
		}
				
		Board clonedBoard = null;
        try {	
        	clonedBoard = (Board) super.clone();
		
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        clonedBoard.setHoles(clonedHoles);
        return clonedBoard;
    }

}
