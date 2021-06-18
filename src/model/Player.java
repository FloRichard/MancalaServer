package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

import exception.NotYourTurnException;
import exception.UnplayableHoleException;


public class Player implements Runnable{
	private Socket socket;
	private PrintWriter outPut;
	private Scanner in;
	
	private Board board;
	private Board LastMove;
	
	private Granary granary;
	private int LastGranaryValue;

	private String name;

	private int playerNumber;
	private int startIndexArea;
	private int endIndexArea;
	private int score;
	private int nbSeedsWonInGame;
	
	private boolean isBlocked;
	
	private boolean isReadyToContinue;
	

	public Player(Socket socket, Board board) {
		System.out.println("Un joueur vient de se connecter");
		this.socket = socket;
		
		try {
			this.in = new Scanner(this.socket.getInputStream());
			this.outPut = new PrintWriter(this.socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.granary = new Granary(0);
		this.LastGranaryValue = 0;

		
		this.score = 0;		
		
		this.board = board;
		
		name = "";
		this.playerNumber = board.setPlayer(this);
		if (playerNumber == 1) {
			startIndexArea = 0;
			endIndexArea = 5;
		}else {
			startIndexArea = 6;
			endIndexArea = 11;
		}
		
		this.nbSeedsWonInGame = 0;
		this.isReadyToContinue = true;

		
		System.out.println("Ce joueur sera le joueur numéro " +this.playerNumber);
	}

	@Override
	public void run() {
		init(false);
		while(true) {
			try {
				String input = this.in.next();
				System.out.println("Player "+this.playerNumber+" is playing.\nRequest : "+input);
	    	 	this.getBoard().handleATurn(this, input);
			} catch(NoSuchElementException e) {
				System.out.println("Le joueur "+this.playerNumber+" s'est déconnecté. En attente de reconnexion");
				
				handleDisconnection();
				try {
					this.socket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Thread.currentThread().interrupt();
				break;
			}
		}
	}
	
	/**
	 * Function called to make players waiting for each other.
	 * @param resetGame specifies if it's called by a reset request.
	 */
	public void init(boolean resetGame) {
		if (resetGame) {
			while(this.getBoard().readyForNewGame.get() != 2) {}
		}else
			while(this.getBoard().isNotFull.get());
		
		this.outPut.println("{\"type\":\"init\",\"playerNumber\":"+this.playerNumber+",\"isBeginning\":"+!this.isBlocked+"}");
	}
	
	/**
	 * Function that handle a move from this player. It checks if every rules
	 * of the game are respected. If not it throws exceptions.
	 * @param holeIndex The index of the hole played by the player.
	 * @throws UnplayableHoleException Thrown when the played hole is not in the player area, or when the move is starving or not feeding the enemy.
	 * @throws NotYourTurnException Thrown when the player send a move but it is not his turn.
	 * @throws EasyModeWin Thrown when player has win in easy mode.
	 */
	public void playAMove(int holeIndex) throws UnplayableHoleException, NotYourTurnException, EasyModeWin {
		if (this.isBlocked) {
			throw new NotYourTurnException();
		}
		
		if (!isInArea(holeIndex)) {
			throw new UnplayableHoleException(UnplayableHoleException.NotYourAreaErr);
		}
		
		if (this.getBoard().getHoles().get(holeIndex).getSeeds() == 0) {
			throw new UnplayableHoleException(UnplayableHoleException.EmptyHoleErr);
		}
		
		if (!moveIsFeedingEnemy(holeIndex)) {
			// Check if we are in the situation of the rule number 8
			if (this.hasWon()) {
				System.out.println("Easy mode win !");
				throw new EasyModeWin("");
			}
			throw new UnplayableHoleException(UnplayableHoleException.MoveIsNotFeedingErr);
		}
		int index = this.getBoard().distribute(holeIndex);	
		if (!moveIsStarvingEnemy(holeIndex)) {
			this.granary.addSeeds(takeSeeds(index, this.getBoard()));
		}
	}
	
	/**
	 * Function that will take seeds from enemy area.
	 * It takes seeds only if the last hole contains 2 or 3 seeds.
	 * Then it goes backward and check if holes have 2 or 3 seeds.
	 * It stops at the first hole that contains more than 3 seeds.
	 * @param index index of the last hole where a seeds has been dropped off.
	 */
	private int takeSeeds(int index, Board b) {
		int nbSeedsToAddIngranary = 0;
		while(b.getHoles().get(index).isRetrievable() && !isInArea(index)) {
			int nbSeedsInTheHole = b.getHoles().get(index).retrieveSeeds();
			nbSeedsToAddIngranary += nbSeedsInTheHole;
			index--;
			if (index < 0) {
				index = b.getHoles().size() -1;
			}
		}
		return nbSeedsToAddIngranary;
	}
	
	/**
	 * Checks if the move is feeding the enemy or not.
	 * The move is feeding the area if the enemy area is not empty or if the played hole
	 * contains enough seeds to reach enemy area
	 * @param playedHoleIndex
	 * @return true if the move is feeding the enemy
	 */
	private boolean moveIsFeedingEnemy(int playedHoleIndex) {
		for(int i=this.getEnemy().startIndexArea; i<=this.getEnemy().endIndexArea; i++) {
			if (this.getBoard().getHoles().get(i).getSeeds() != 0) {
				return true;
			}
		}
		
		if(this.getBoard().getHoles().get(playedHoleIndex).getSeeds() + playedHoleIndex <= this.endIndexArea) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the move is starving the enemy.
	 * The move is starving the enemy if it takes all the seeds of the enemy.
	 * @param playedHoleIndex
	 * @return
	 */
	private boolean moveIsStarvingEnemy(int playedHoleIndex) {
		Board clonedBoard = this.getBoard().clone();
		int lastIndex = clonedBoard.distribute(playedHoleIndex);
		takeSeeds(lastIndex, clonedBoard);
		for(int i=this.getEnemy().startIndexArea; i<=this.getEnemy().endIndexArea; i++) {
			if (clonedBoard.getHoles().get(i).getSeeds() != 0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the player has won the round regarding the difficulty.
	 * If the difficulty is set to beginner, then a round is won when the total
	 * number of seeds is lower than 6 and if the enemy has no seeds in its area.
	 * If the difficulty is set to normal, the round is win when the player has more than 24 seeds
	 * in its granary.
	 * Normal difficulty win conditions is also applied to easy mode.
	 * @return true is the round is won.
	 */
	public boolean hasWon() {
		if (this.getBoard().isBeginnerDifficulty()) {
			if (this.getBoard().getSeeds() > 6) {
				return false;
			}
			if (this.getEnemy().getSeedsInArea() == 0) {
				return true;
			}
		}
		
		return this.granary.getSeeds() >= 25;
	}
	
	/**
	 * Checks if the hole played is in the playable area of the player.
	 * @param holeIndex index of the played hole.
	 * @return return true if the hole is in the player area.
	 */
	private boolean isInArea(int holeIndex) {
		return this.startIndexArea <= holeIndex && this.endIndexArea >= holeIndex;
	}
	
	/**
	 * Calculates the number of seeds contained in the area of the player.
	 * @return number of seeds contained in player area.
	 */
	public int getSeedsInArea() {
		int nbSeedsInArea = 0;
		for(int i=this.startIndexArea; i<=this.endIndexArea; i++) {
			nbSeedsInArea += this.getBoard().getHoles().get(i).getSeeds();
		}
		
		return nbSeedsInArea;
	}
	
	/**
	 * Gets the enemy of this player
	 * @return the player enemy
	 */
	public Player getEnemy() {
		if (this.playerNumber == 1 ) {
			return this.getBoard().getPlayerTwo();
		}else {
			return this.getBoard().getPlayerOne();
		}
	}
	
	/**
	 * Handles player disconnection from the server.
	 * When one player leave, it sends to its enemy an error.
	 * If both players are disconnected, the board can accept connections again.
	 */
	public void handleDisconnection() {
		try {
			this.getEnemy().getOutPut().println("{\"type\":\"error\",\"value\":\"error.player"+this.playerNumber+".disconnection\"}");
			
		}catch(NullPointerException e) {
			System.out.println("Les deux joueurs sont déconnectés");
			this.getBoard().emptyBoard();
			this.getBoard().isNotFull.set(true);
			if (this.playerNumber == 1) {
				this.getBoard().setPlayerOne(null);
			}else {
				this.getBoard().setPlayerTwo(null);
			}
		}
		this.getBoard().isNotFull.set(true);
		if (this.playerNumber == 1) {
			this.getBoard().setPlayerOne(null);
		}else {
			this.getBoard().setPlayerTwo(null);
		}
		try {
			finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	public Granary getGranary() {
		return this.granary;
	}
	
	public Board getLastMove() {
		return LastMove;
	}

	public void setLastMove(Board lastMove) {
		LastMove = lastMove;
	}
	
	public int getPlayerNumber() {
		return playerNumber;
	}

	public void setPlayerNumber(int playerNumber) {
		this.playerNumber = playerNumber;
	}

	public int getStartIndexArea() {
		return startIndexArea;
	}

	public void setStartIndexArea(int startIndexArea) {
		this.startIndexArea = startIndexArea;
	}

	public int getEndIndexArea() {
		return endIndexArea;
	}

	public void setEndIndexArea(int endIndexArea) {
		this.endIndexArea = endIndexArea;
	}

	public boolean isBlocked() {
		return isBlocked;
	}

	public void setBlocked(boolean isBlocked) {
		this.isBlocked = isBlocked;
	}

	public void setGranary(Granary granary) {
		this.granary = granary;
	}
	
	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public PrintWriter getOutPut() {
		return outPut;
	}

	public void setOutPut(PrintWriter outPut) {
		this.outPut = outPut;
	}
	
	public int getScore() {
		return score;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public void addPointToScore() {
		this.score++;
	}
	
	public boolean isReadyToContinue() {
		return isReadyToContinue;
	}

	public void setReadyToContinue(boolean isReadyToContinue) {
		this.isReadyToContinue = isReadyToContinue;
	}

	public Scanner getIn() {
		return in;
	}

	public void setIn(Scanner in) {
		this.in = in;
	}
	
	public int getLastGranaryValue() {
		return LastGranaryValue;
	}

	public void setLastGranaryValue(int lastGranaryValue) {
		LastGranaryValue = lastGranaryValue;
	}
	
	public int getNbSeedsWonInGame() {
		return nbSeedsWonInGame;
	}

	public void setNbSeedsWonInGame(int nbSeedsWonInGame) {
		this.nbSeedsWonInGame = nbSeedsWonInGame;
	}
	
	public void addNbSeedsWonInGame(int nbSeedsWonInGameToAdd) {
		this.nbSeedsWonInGame += nbSeedsWonInGameToAdd;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
