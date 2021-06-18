package model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import exception.NotYourTurnException;
import exception.UnplayableHoleException;

/**
 * Main class of the game. This class represents the board and can handle incoming requests from players.
 * Both players has the same reference on the board, and the board knows both players.
 * 
 * @author Florian RICHARD
 * @author Julien MONTEIL
 */
public class Board implements Cloneable{
	/**
	 * The 12 holes that are on the board.
	 */
	private ArrayList<Hole> holes;
	
	/**
	 * The first player to connect to game.
	 */
	private Player playerOne;
	
	/**
	 * The second player to connect to the game.
	 */
	private Player playerTwo;
	private boolean isBeginnerDifficulty;
	
	/**
	 * Indicates if the server wait for players confirmation to end the round.
	 */
	private volatile boolean needEndRoundConfirmation;

	/**
	 * Indicates if the board is full or not.
	 */
	public AtomicBoolean isNotFull;
	
	/**
	 * Indicates if players are ready to play a new game.
	 */
	public AtomicInteger readyForNewGame;
	
	private AtomicInteger numberOfRoundPlayed;
	private final String ROUND = "round";
	private final String GAME = "game";
	private final String SCORE_PATH = "score.txt";
	
	public Board(ArrayList<Hole> holes) {
		this.holes = holes;
		this.numberOfRoundPlayed = new AtomicInteger(0);
		this.isBeginnerDifficulty = false; // default difficulty
		this.needEndRoundConfirmation = false;
		this.isNotFull = new AtomicBoolean(true);
		readyForNewGame = new AtomicInteger(0);
	}
	
	/**
	 * Set playerOne and playerTwo of the board.
	 * The first player to connect to the server will be the playerOne.
	 * The second will be set to player two.
	 * The first player to play is chosen randomly.
	 * When the second player is set the board is full so isNotFull is set to false.
	 * @param player the player that will be set.
	 * @return the number of the player (1 or 2)
	 */
	public int setPlayer(Player player) {
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
			
			this.isNotFull.set(false);
			return 2;
		}
	}
	
	/**
	 * Function that is used as a controller of client inputs.
	 * It is called by each player when they want to interact with the server.
	 * Request are JSON string that is parsed with the ClientInputController. See more detail
	 * in ClientInputControllerDocumentation.
	 * @param player the player that is actually playing.
	 * @param input the request of the player.
	 * @see ClientInputController
	 */
	public void handleATurn(Player player, String input) {
		ClientInputController request = new ClientInputController(input);
		if(request.isPlayerName()) {
			player.setName(request.getPlayerName());
			return;
		}
		
		if (request.isNewGame()) {
			player.setReadyToContinue(true);
			if (player.getEnemy().isReadyToContinue()) {
				this.resetBoard(player);
				if(player.isBlocked())
					this.broadcastMsg( getBoardToJSONString(this, player.getEnemy(), false));
				else
					this.broadcastMsg( getBoardToJSONString(this, player, false));
				player.setReadyToContinue(false);
				player.getEnemy().setReadyToContinue(false);
				player.setBoard(this);
				player.getEnemy().setBoard(this);
			}
			return;
		}
		
		if (request.isLoading()) {
			System.out.println("Loading a board...");
			this.resetBoard(player);
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
			return;
 		}
		
		if (request.isAMove()){
			player.setLastMove(this.clone());
			player.setLastGranaryValue(player.getGranary().getSeeds());
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
 			return;
 		}
		
		if (request.isAConfirmation() && request.getConfirmationAction().equals("abort")) {
			player.setBoard(player.getLastMove());
			player.getGranary().setSeeds(player.getLastGranaryValue());
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
			System.out.println("null round");
			player.addPointToScore();
			player.getEnemy().addPointToScore();
			
			// Add to rounds when the match is null because each player wins one point.
			this.addARound();
			this.addARound();
			if (this.numberOfRoundPlayed.get() == 6) {
				System.out.println("Game is over !");
				gameOver(player);
				return;
			}else {
				emptyBoard();
			}
			this.broadcastMsg(this.getDrawJSONStringOn(ROUND));
		}
		
		player.setBoard(this);
		player.getEnemy().setBoard(this);
		this.broadcastMsg(getBoardToJSONString(this, player.getEnemy(), false));
		
		// Unlocking the enemy, locking the actual player.
		player.getEnemy().setBlocked(false);
		player.setBlocked(true);	
		
	
	}
	
	/**
	 * Handles the end of a round and checks if the game is over.
	 * This function will make the actual player and it's enemy waiting for a confirmation to continue playing.
	 * @param player the player that is playing.
	 * @param isSurrend specifies if the round has been won by surrending or not.
	 * @return Returns true if the game is over.
	 */
	public boolean handleWin(Player player, boolean isSurrend) {
		player.addPointToScore();
		this.addARound();
		player.addNbSeedsWonInGame(player.getGranary().getSeeds());
		
		player.getEnemy().getOutPut().println(getBoardToJSONString(this, player.getEnemy(), false));
		if (isSurrend) {
			player.getOutPut().println(getBoardToJSONString(this, player.getEnemy(), false));
		}
		
		if (isGameOver(player)) {
			System.out.println("Game is over !");
			gameOver(player);
			return true;
		}
		
		System.out.println("getwinJSON string" + this.getWinJSONStringOn(ROUND));
		player.getOutPut().println(this.getWinJSONStringOn(ROUND));
		player.getEnemy().getOutPut().println(this.getLoseJSONStringOn(ROUND));
		
		this.needEndRoundConfirmation = true;
		return false;
	}
	
	/**
	 * Indicates if the game is over. A game is over when one of the player's score is over 3 or
	 * if both player have 3 won rounds.
	 * @param p the player that won the round.
	 * @return true if the game is over, false else.
	 */
	private boolean isGameOver(Player p) {
		return (Math.abs(p.getScore() - p.getEnemy().getScore()) >= 3 && this.numberOfRoundPlayed.get() >3) || 
				(p.getScore() == 3 && p.getEnemy().getScore() == 3);
	}
	
	/**
	 * Handles the end of a game, when 6 round has been played.
	 * It sends appropriate messages to each player regarding if the match is null, won or lose.
	 * Write the score in the score File.
	 * @param player the player that end the game by playing.
	 */
	public void gameOver(Player player) {	
		String scoreToSend = saveScore(player);
		if (player.getScore() > 3) {
			player.getOutPut().println(getWinJSONStringOn(GAME, scoreToSend));
			player.getEnemy().getOutPut().println(getLoseJSONStringOn(GAME, scoreToSend));
		}else if (player.getScore() == 3) {
			player.getOutPut().println(getDrawJSONStringOn(GAME, scoreToSend));
			player.getEnemy().getOutPut().println(getDrawJSONStringOn(GAME, scoreToSend));
		}else {
			player.getOutPut().println(getLoseJSONStringOn(GAME, scoreToSend));
			player.getEnemy().getOutPut().println(getWinJSONStringOn(GAME, scoreToSend));
		}
		
		
		player.setReadyToContinue(true);
		player.getEnemy().setReadyToContinue(true);
		
	}
	/**
	 * Saves actual the winner number of seeds in score.txt file.
	 * Returns the sorted list of score
	 * @param p the player that has won.
	 * @return the sorted score list in HTML format.
	 */
	public String saveScore(Player p) {
		String data = p.getNbSeedsWonInGame() + ":"+p.getName()+"\n";
		String scoreToSend = "<ol>";
		
		FileWriter f;
		try {
			f = new FileWriter(SCORE_PATH, true);
			f.write(data);
			f.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			FileReader fr = new FileReader(SCORE_PATH);
			BufferedReader reader = new BufferedReader(fr);
			ArrayList<String> scores = new ArrayList<>();
			String line = "";
			while((line=reader.readLine())!=null){
				scores.add(line);
			}
			Collections.sort(scores);
			
			
			for (String score : scores) {
				scoreToSend += "<li>" +score+"</li>"; 
			}
			scoreToSend += "</ol>";
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return scoreToSend;
	}
	
	/**
	 * Checks if the round is a null.
	 * @return true if the round is null.
	 */
	public boolean isNullRound() {
		if (this.getSeeds() < 6) {
			return this.getPlayerOne().getGranary().getSeeds() <= 24  && this.getPlayerTwo().getGranary().getSeeds() <= 24;
		}
		return false;
	}
	
	/**
	 * Loads a board from the client request.
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
		this.numberOfRoundPlayed.set(request.getP1Score() + request.getP2Score());
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
	 * Distributes seeds from the played hole.
	 * @param holeIndex the hole played by the player
	 * @return the index of the hole where the last seed goes.
	 */
	public int distribute(int holeIndex) {
		int nbSeeds = this.getHoles().get(holeIndex).getSeeds();
		this.getHoles().get(holeIndex).retrieveSeeds();
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
	 * Sends a message to both players.
	 * @param msg the message to send to players.
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
	 * Gets the total number of seeds in board holes(excepted granaries).
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
	 * This function resets the board.
	 * It set holes and granary to default values, set players' scores and the number of rounds played to 0.
	 * The it set's readyForNewGame to 0, players will have to wait each other.
	 * @param p the player that is playing.
	 */
	public void resetBoard(Player p) {
		emptyBoard();
		
		p.setScore(0);
		p.getEnemy().setScore(0);
		
		p.setNbSeedsWonInGame(0);
		p.getEnemy().setNbSeedsWonInGame(0);
		
		p.getGranary().setSeeds(0);
		p.getEnemy().getGranary().setSeeds(0);
		
		this.numberOfRoundPlayed.set(0);
		this.readyForNewGame.set(0);
	}
	
	/**
	 * Empties the board by reseting holes and granaries.
	 */
	public void emptyBoard() {
		for(int i =0;i<12;i++) {
			this.getHoles().get(i).setSeeds(4);
		}
		this.emptyGranaries();
	}
	
	/**
	 * Empties both players' granaries.
	 */
	public void emptyGranaries() {
		if (this.getPlayerOne() != null) {
			this.getPlayerOne().getGranary().retrieveSeeds();
		}
		if (this.getPlayerTwo() != null) {
			this.getPlayerTwo().getGranary().retrieveSeeds();
		}
	}
	
	/**
	 * Creates a JSON string representation of the actual board.
	 * @param b the board to represent in JSON.
	 * @param p the player that is playing.
	 * @param confirmation specifies if the player p need to confirm.
	 * @return the JSON string.
	 */
	public static String getBoardToJSONString(Board b, Player p, boolean confirmation) {
		String JSONHoles = "[";
		for (int i = 0; i< b.getHoles().size();i++) {
			JSONHoles += b.getHoles().get(i).getSeeds();
			if (i < b.getHoles().size() -1) {
				JSONHoles += ",";
			}
		}
		JSONHoles += "]";
		String difficulty;
		if (b.isBeginnerDifficulty) {
			difficulty ="easy";
		}else {
			difficulty ="normal";
		}
		String boardJSON = "{\"type\":\"board\",\"seeds\":"+JSONHoles+
				",\"playerOneGranaryCount\":"+b.getPlayerOne().getGranary().getSeeds()+
				",\"playerTwoGranaryCount\":"+b.getPlayerTwo().getGranary().getSeeds()+
				",\"playerOneScore\":"+b.getPlayerOne().getScore()+
				",\"playerTwoScore\":"+b.getPlayerTwo().getScore()+
				",\"playerNumber\":"+p.getPlayerNumber()+
				",\"needConfirmation\":"+confirmation+
				",\"difficulty\":\""+difficulty+"\""+
				"}";
		return boardJSON;
	}

	public String getWinJSONStringOn(String target, String... scoreToSend) {
		if (target.equals(GAME)) {
			return "{\"type\":\"info\",\"value\":\"info.win."+target+"\",\"score\":\""+scoreToSend[0]+"\"}";
		}
		return "{\"type\":\"info\",\"value\":\"info.win."+target+"\"}";
	}
	
	public String getLoseJSONStringOn(String target,String... scoreToSend) {
		if (target.equals(GAME)) {
			return "{\"type\":\"info\",\"value\":\"info.lose."+target+"\",\"score\":\""+scoreToSend[0]+"\"}";
		}
		return "{\"type\":\"info\",\"value\":\"info.lose."+target+"\"}";
	}
	
	public String getDrawJSONStringOn(String target,String... scoreToSend) {
		if (target.equals(GAME)) {
			return "{\"type\":\"info\",\"value\":\"info.draw."+target+"\",\"score\":\""+scoreToSend[0]+"\"}";
		}
		return "{\"type\":\"info\",\"value\":\"info.draw."+target+"\"}";
	}
	
	
	public ArrayList<Hole> getHoles() {
		return holes;
	}
	public void setHoles(ArrayList<Hole> holes) {
		this.holes = holes;
	}
	
	public Player getPlayerOne() {
		return playerOne;
	}
	public void setPlayerOne(Player playerOne) {
		this.playerOne = playerOne;
	}
	
	public Player getPlayerTwo() {
		return playerTwo;
	}
	public void setPlayerTwo(Player playerTwo) {
		this.playerTwo = playerTwo;
	}
	
	public boolean isBeginnerDifficulty() {
		return isBeginnerDifficulty;
	}

	public void setBeginnerDifficulty(boolean isBeginnerDifficulty) {
		this.isBeginnerDifficulty = isBeginnerDifficulty;
	}
	

	public void addARound() {
		this.numberOfRoundPlayed.addAndGet(1);
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
