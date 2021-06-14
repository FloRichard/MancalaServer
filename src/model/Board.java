package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;


public class Board implements Cloneable{
	private ArrayList<Hole> holes;
	private SimplePlayer playerOne;
	private SimplePlayer playerTwo;
	private boolean isBeginnerDifficulty;
	private boolean isFull;

	private int numberOfRoundPlayed;

	public Board(ArrayList<Hole> holes) {
		this.holes = holes;
		this.numberOfRoundPlayed = 0;
		this.isBeginnerDifficulty = true; // default difficulty
		this.isFull = false;
	}
	
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
	
	public void handleATurn(SimplePlayer player, String input, PrintWriter out) {
		ClientInputController request = new ClientInputController(input);
		if (request.isNewGame()) {
			ArrayList<Hole> holes = new ArrayList<Hole>();
			for(int i =0;i<12;i++) {
				 Hole newH = new Hole(4);
				 holes.add(newH);
			}
			this.setHoles(holes);
			this.emptyGranaries();
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
				out.println(e.getMessage());
				System.out.println(e.getMessage());
				return;
			} catch (EasyModeWin e) {
				if (handleWin(player)) {
					return;
				}
			}
 			System.out.println("lastMove "+ getBoardToJSONString(player.getLastMove()));
 			out.println(getBoardToJSONString(this));
 			return;
 		}
		
		if (request.isAConfirmation() && request.getConfirmationAction().equals("abort")) {
			player.setBoard(player.getLastMove().clone());
			System.out.println("Aborting the move... Returning to"+ getBoardToJSONString(player.getLastMove()));
 			out.println(getBoardToJSONString(this));
 			return;
 		}
		
		if (player.hasWon() && handleWin(player) ) {
			return;
	 	}
		
		if (this.isNullGame()) {
			this.broadcastMsg("Match nul");
		}
		
		this.broadcastMsg(getBoardToJSONString(this));
		
		if (player.getPlayerNumber() == 1) {
			this.getPlayerTwo().setBlocked(false);
		}else {
			this.getPlayerOne().setBlocked(false);
		}
		
		player.setBlocked(true);
	}
	
	// Return true if the game is over (if 6 round has been played)
	public boolean handleWin(SimplePlayer player) {
		this.broadcastMsg("Le joueur "+player.getPlayerNumber()+" a gagné la manche");
		player.addPointToScore();
		this.addARound();
		if (this.getNumberOfRoundPlayed() == 6) {
			System.out.println("Game is over !");
			gameOver(player.getPlayerNumber());
			return true;
		}
		return false;
	}
	
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
	
	public void informEnemyOfDisconnection(int playerNumber) {
		String disconnectionMsg = "ERR: player "+ playerNumber + " has left. You won.";
		try {
			if (playerNumber == 1) {
				PrintWriter outTwo = new PrintWriter(this.getPlayerTwo().getSocket().getOutputStream(), true);
				outTwo.println(disconnectionMsg);
			}else {
				PrintWriter outOne = new PrintWriter(this.getPlayerOne().getSocket().getOutputStream(), true);
				outOne.println(disconnectionMsg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean isNullGame() {
		if (this.getSeeds() < 6) {
			return this.getPlayerOne().getGranary().getSeeds() <= 24  && this.getPlayerTwo().getGranary().getSeeds() <= 24;
		}
		return false;
	}
	
	public void gameOver(int playerNumber) {
		String outputMessagePlayerOne = null;
		String outputMessagePlayerTwo = null;
		PrintWriter outOne = null;
		PrintWriter outTwo = null;
		try {
			outOne = new PrintWriter(this.getPlayerOne().getSocket().getOutputStream(), true);
			outTwo = new PrintWriter(this.getPlayerTwo().getSocket().getOutputStream(), true);
		} catch (IOException e) {
			System.err.println("Can't get output streams");
			e.printStackTrace();
		}
		
		if (playerNumber == this.getPlayerOne().getPlayerNumber()) {
			if (getPlayerOne().getScore() > 3) {
				outputMessagePlayerOne = getGameWinJSONString();
				outputMessagePlayerTwo = getGameLoseJSONString();
			}else if (getPlayerOne().getScore() == 3) {
				outputMessagePlayerOne = getGameNullJSONString();
				outputMessagePlayerTwo = getGameNullJSONString();
			}else {
				outputMessagePlayerOne = getGameLoseJSONString();
				outputMessagePlayerTwo = getGameWinJSONString();
			}
		}else {
			if (getPlayerTwo().getScore() > 3) {
				outputMessagePlayerTwo = getGameWinJSONString();
				outputMessagePlayerOne = getGameLoseJSONString();
			}else if (getPlayerTwo().getScore() == 3) {
				outputMessagePlayerTwo = getGameNullJSONString();
				outputMessagePlayerOne = getGameNullJSONString();
			}else {
				outputMessagePlayerTwo = getGameLoseJSONString();
				outputMessagePlayerOne = getGameWinJSONString();
			}
		}
		outOne.println(outputMessagePlayerOne);
		outTwo.println(outputMessagePlayerTwo);
	}
	
	public int getSeeds() {
		int nbSeed = 0;
		for (Hole hole : holes) {
			nbSeed += hole.getSeeds();
		}
		return nbSeed;
	}
	
	public void emptyGranaries() {
		this.getPlayerOne().getGranary().removeSeeds();
		this.getPlayerTwo().getGranary().removeSeeds();
	}
	
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

	public String getGameWinJSONString() {
		return "{\"info\":\"win\"}";
	}
	
	public String getGameLoseJSONString() {
		return "{\"info\":\"lose\"}";
	}
	
	public String getGameNullJSONString() {
		return "{\"info\":\"null\"}";
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
