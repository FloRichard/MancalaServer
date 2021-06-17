package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import exception.NotYourTurnException;
import exception.UnplayableHoleException;


public class Board implements Cloneable{
	private ArrayList<Hole> holes;
	private SimplePlayer playerOne;
	private SimplePlayer playerTwo;
	private boolean isBeginnerDifficulty;
	private volatile boolean needEndRoundConfirmation;

	public AtomicBoolean isNotFull;
	public AtomicInteger readyForNewGame;
	
	private final String ROUND = "round";
	private final String GAME = "game";

	private int numberOfRoundPlayed;

	public Board(ArrayList<Hole> holes) {
		this.holes = holes;
		this.numberOfRoundPlayed = 0;
		this.isBeginnerDifficulty = false; // default difficulty
		this.needEndRoundConfirmation = false;
		this.isNotFull = new AtomicBoolean(true);
		readyForNewGame = new AtomicInteger(0);
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
			return 1;
		}else {
			int firstPlayerToPlay = 1 + (int)(Math.random() * ((2 - 1) + 1));
			
			this.playerTwo = player;
			if (firstPlayerToPlay == 1) {
				this.playerOne.setBlocked(false);
				this.playerTwo.setBlocked(true);
			}else {
				this.playerOne.setBlocked(true);
				this.playerTwo.setBlocked(false);
			}
//			this.playerOne.setBlocked(false);
//		this.playerTwo.setBlocked(true);
//			this.setNumberOfRoundPlayed(5);
//			this.playerOne.setScore(3);
//			this.playerTwo.setScore(2);
//			System.out.println("p2 svcore = "+this.playerTwo.getScore()+"  "+ player.getScore());
			
//			this.playerOne.getGranary().setSeeds(25);
			
			this.isNotFull.set(false);
			return 2;
		}
	}
	
	/**
	 * Function that is used as a controller of client inputs.
	 * It is called by each player when they want to interact with the server.
	 * @param player the player that is playing.
	 * @param input the input of the player.
	 * @param out the output stream of the player.
	 */
	public void handleATurn(SimplePlayer player, String input) {
		ClientInputController request = new ClientInputController(input);
		System.out.println(player.getPlayerNumber()+ "is blocked = "+player.isBlocked()+ " is ready to continue = "+player.isReadyToContinue());
		//System.out.println("\tactual board\n\t\t"+ getBoardToJSONString(this, player, true));
		if (request.isNewGame()) {
			player.setReadyToContinue(true);
			if (player.getEnemy().isReadyToContinue()) {
				emptyBoard();
				player.setScore(0);
				player.getEnemy().setScore(0);
				this.setNumberOfRoundPlayed(0);
				this.readyForNewGame.set(0);
				if(player.isBlocked())
					this.broadcastMsg( getBoardToJSONString(player.getBoard(), player.getEnemy(), false));
				else
					this.broadcastMsg( getBoardToJSONString(player.getBoard(), player, false));
				player.setReadyToContinue(false);
				player.getEnemy().setReadyToContinue(false);
			}
			return;
		}
		
		if (request.isReconnection()) {
			player.getOutPut().println(getBoardToJSONString(this, player, false));
			return;
		}
		
		if (request.isLoading()) {
			System.out.println("Loading a board...");
			this.loadFromRequest(request);
			this.broadcastMsg(getBoardToJSONString(this, player, false));
			System.out.println("Board loaded !");
			return;
		}
		
		if (request.isDifficultyChoice()) {
			if (request.isBeginnerDifficulty()) {
				this.setBeginnerDifficulty(true);
			}else {
				this.setBeginnerDifficulty(false);
			}
 		}
		
		if (request.isAMove()){
			//TODO cloning granary
			player.setLastMove(this.clone());
			player.setLastGranaryValue(player.getGranary().getSeeds());
			//System.out.println("\tlastMove\n\t\t"+ getBoardToJSONString(player.getLastMove(), player, true));
 			try {
				player.playAMove(request.getHoleIndexPlayed());
			} catch (UnplayableHoleException | NotYourTurnException e) {
				player.getOutPut().println(e.getMessage());
				System.out.println(e.getMessage());
				return;
			} catch (EasyModeWin e) {
				if (handleWin(player, false)) {
					return;
				}
			}
 			
 			player.getOutPut().println(getBoardToJSONString(this, player, true));
 			//System.out.println("\tafter move\n\t\t"+ getBoardToJSONString(this, player, true));
 			return;
 		}
		
		if (request.isAConfirmation() && request.getConfirmationAction().equals("abort")) {
			player.setBoard(player.getLastMove());
			player.getGranary().setSeeds(player.getLastGranaryValue());
			System.out.println("\tAborting the move... Returning to :\n\t\t"+ getBoardToJSONString(player.getBoard(), player, true));
			player.getOutPut().println(getBoardToJSONString(player.getBoard(), player, false));
 			return;
 		}
		
		if (request.isEndRoundConfirmation()){
			player.setReadyToContinue(true);
			if (player.getEnemy().isReadyToContinue()) {
				emptyBoard();
				this.broadcastMsg( getBoardToJSONString(player.getBoard(), player, false));
				player.setReadyToContinue(false);
				player.getEnemy().setReadyToContinue(false);
			}
			return;
		}
		
		if (request.isSurrend()) {
			player.getEnemy().getGranary().setSeeds(this.getSeeds());
			for(int i=0; i<this.getHoles().size(); i++) {
				this.getHoles().get(i).retrieveSeeds();
			}
			if(handleWin(player.getEnemy(), true)) {
				return;
			}
			player.getEnemy().setBlocked(false);
			player.setBlocked(true);
			return;
		}
		
		if(request.isReset()) {
			this.readyForNewGame.addAndGet(1);
			player.init(true);
			return;
		}
		
		if (player.hasWon()) {
			
			if(handleWin(player, false)) {
				return;
			}
	 	}
		
		if (this.isNullRound()) {
			player.addPointToScore();
			player.getEnemy().addPointToScore();
			
			// Add to rounds when the match is null because each player wins one point.
			this.addARound();
			this.addARound();
			if (this.getNumberOfRoundPlayed() == 6) {
				System.out.println("Game is over !");
				gameOver(player);
				return;
			}else {
				emptyBoard();
			}
			this.broadcastMsg(this.getDrawJSONStringOn(ROUND));
		}
		
		System.out.println("Broadcasted board = "+ getBoardToJSONString(this, player, true));
		player.setBoard(this);
		player.getEnemy().setBoard(this);
		this.broadcastMsg(getBoardToJSONString(this, player.getEnemy(), false));
		
		
		// Unlocking the enemy, locking the actual player.
		player.getEnemy().setBlocked(false);
		player.setBlocked(true);	
		
	
	}
	
	/**
	 * Handle the end of a round and check if the game is over.
	 * This function wait for user confirmation to continue playing.
	 * @param player the player that is playing.
	 * @param isSurrend specify if the win is by surrending.
	 * @return Returns true if the game is over.
	 */
	public boolean handleWin(SimplePlayer player, boolean isSurrend) {
		player.addPointToScore();
		this.addARound();
		System.out.println("nb of round played = "+this.getNumberOfRoundPlayed());
		player.getEnemy().getOutPut().println(getBoardToJSONString(this, player.getEnemy(), false));
		if (isSurrend) {
			player.getOutPut().println(getBoardToJSONString(this, player.getEnemy(), false));
		}
		if ((Math.abs(player.getScore() - player.getEnemy().getScore()) >= 3 && this.getNumberOfRoundPlayed() >3) || (player.getScore() == 3 && player.getEnemy().getScore() == 3)) {
			System.out.println("Game is over !");
			gameOver(player);
			return true;
		}
		
		player.getOutPut().println(this.getWinJSONStringOn(ROUND));
		player.getEnemy().getOutPut().println(this.getLoseJSONStringOn(ROUND));
		
		this.needEndRoundConfirmation = true;
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
			player.getOutPut().println(getDrawJSONStringOn(GAME));
			player.getEnemy().getOutPut().println(getDrawJSONStringOn(GAME));
		}else {
			player.getOutPut().println(getLoseJSONStringOn(GAME));
			player.getEnemy().getOutPut().println(getWinJSONStringOn(GAME));
		}
		
//		player.setBlocked(false);
//		player.getEnemy().setBlocked(false);
		player.setReadyToContinue(true);
		player.getEnemy().setReadyToContinue(true);
		//player.getEnemy().init(true);
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
	 * Empty both players' granary.
	 */
	public void emptyGranaries() {
		if (this.getPlayerOne() != null) {
			this.getPlayerOne().getGranary().removeSeeds();
		}
		if (this.getPlayerTwo() != null) {
			this.getPlayerTwo().getGranary().removeSeeds();
		}
	}
	
	/**
	 * Creates a JSON string representation of the actual board.
	 * @param b the board to represent in JSON.
	 * @return the JSON string.
	 */
	public static String getBoardToJSONString(Board b, SimplePlayer p, boolean confirmation) {
		String JSONHoles = "[";
		for (int i = 0; i< b.getHoles().size();i++) {
			JSONHoles += b.getHoles().get(i).getSeeds();
			if (i < b.getHoles().size() -1) {
				JSONHoles += ",";
			}
		}
		JSONHoles += "]";
		
		String boardJSON = "{\"type\":\"board\",\"seeds\":"+JSONHoles+
				",\"playerOneGranaryCount\":"+b.getPlayerOne().getGranary().getSeeds()+
				",\"playerTwoGranaryCount\":"+b.getPlayerTwo().getGranary().getSeeds()+
				",\"playerOneScore\":"+b.getPlayerOne().getScore()+
				",\"playerTwoScore\":"+b.getPlayerTwo().getScore()+
				",\"playerNumber\":"+p.getPlayerNumber()+
				",\"needConfirmation\":"+confirmation+
				"}";
		return boardJSON;
	}

	public String getWinJSONStringOn(String target) {
		return "{\"type\":\"info\",\"value\":\"info.win."+target+"\"}";
	}
	
	public String getLoseJSONStringOn(String target) {
		return "{\"type\":\"info\",\"value\":\"info.lose."+target+"\"}";
	}
	
	public String getDrawJSONStringOn(String target) {
		return "{\"type\":\"info\",\"value\":\"info.draw."+target+"\"}";
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
		this.numberOfRoundPlayed = this.numberOfRoundPlayed +1;
		System.out.println("adding round = "+this.numberOfRoundPlayed);
	}
	
	
	public boolean isNeedEndRoundConfirmation() {
		return needEndRoundConfirmation;
	}

	public void setNeedEndRoundConfirmation(boolean needEndRoundConfirmation) {
		this.needEndRoundConfirmation = needEndRoundConfirmation;
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
			e.printStackTrace();
		}
        clonedBoard.setHoles(clonedHoles);
        return clonedBoard;
    }

}
