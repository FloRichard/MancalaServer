package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import com.google.gson.Gson;


public class SimplePlayer implements Runnable{
	private Socket socket;
	private Board board;
	private Board LastMove;
	private Granary granary;
	private int playerNumber;
	private int startIndexArea;
	private int endIndexArea;
	private boolean isBlocked;
	private int score;

	public SimplePlayer(Socket socket, Board board) {
		System.out.println("Un joueur vient de se connecter");
		this.granary = new Granary(0);
		this.socket = socket;
		this.board = board;
		this.playerNumber = board.setPlayer(this);
		if (playerNumber == 1) {
			startIndexArea = 0;
			endIndexArea = 5;
		}else {
			startIndexArea = 6;
			endIndexArea = 11;
		}
		
		this.score = 0;

		System.out.println("Ce joueur sera le joueur numéro " +this.playerNumber);
	}

	@Override
	public void run() {
		while(true) {
			try {
				
				Scanner in = new Scanner(socket.getInputStream());
				PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
				String input = in.next();
				System.out.println(input);
				System.out.println("Player "+this.playerNumber+" is playing");
				//System.out.println("\tActual board :\n\t"+ Board.getBoardToJSONString(this.getBoard()));
	    	 	
	    	 	this.getBoard().handleATurn(this, input, out);
	    	 
			} catch (IOException e) {
				e.printStackTrace();
			} catch(NoSuchElementException e) {
				System.out.println("Le joueur "+this.playerNumber+" s'est déconnecté. Abandon de la partie");
				this.getBoard().informEnemyOfDisconnection(playerNumber);
				break;
			}
			System.out.println("\tBoard after playing :\n\t"+ Board.getBoardToJSONString(this.getBoard()));
			
		}
		
	}
	
	public void playAMove(int holeIndex) throws UnplayableHoleException, NotYourTurnException, EasyModeWin {
		String errMsg;
		if (this.isBlocked) {
			errMsg = "ERR: it's not your turn";
			throw new NotYourTurnException(errMsg);
		}
		
		if (!isInArea(holeIndex)) {
			errMsg = "ERR: this hole is not in your area";
			throw new UnplayableHoleException(errMsg);
		}
		
		if (this.getBoard().getHoles().get(holeIndex).getSeeds() == 0) {
			errMsg = "ERR: Empty hole";
			throw new UnplayableHoleException(errMsg);
		}
		
		if (!moveIsFeedingEnemy(holeIndex)) {
			errMsg = "ERR: move is not feeding enemy";
			throw new UnplayableHoleException(errMsg);
		}
		
		if (moveIsStarvingEnemy(holeIndex)) {
			// Check if we are in the situation of the rule number 8
			if (this.hasWon()) {
				System.out.println("Easy mode win !");
				throw new EasyModeWin("");
			}
			errMsg = "ERR: move is starving the enemy";
			throw new UnplayableHoleException(errMsg);
		}
		
		int index = this.getBoard().distribute(holeIndex);	
		takeSeeds(index);
	}
	
	private void takeSeeds(int index) {
		while(this.getBoard().getHoles().get(index).isRetrievable() && !isInArea(index)) {
			if (index < 0) {
				index = this.getBoard().getHoles().size() -1;
			}
			int nbSeedsInTheHole = this.getBoard().getHoles().get(index).retrieveSeeds();
			this.granary.addSeeds(nbSeedsInTheHole);
			index--;
		}
	}
	
	private boolean moveIsFeedingEnemy(int playedHoleIndex) {
		for(int i=this.startIndexArea; i<=this.endIndexArea; i++) {
			if (this.getBoard().getHoles().get(i).getSeeds() != 0) {
				return true;
			}
		}
		
		if(this.getBoard().getHoles().get(playedHoleIndex).getSeeds() + playedHoleIndex <= this.endIndexArea) {
			return false;
		}
		
		return true;
	}
	
	private boolean moveIsStarvingEnemy(int playedHoleIndex) {
		int cpt = 0;
		Board clonedBoard = this.getBoard().clone();
		clonedBoard.distribute(playedHoleIndex);
		
		int lastIndex = 11;
		while(clonedBoard.getHoles().get(lastIndex).isRetrievable() && !isInArea(lastIndex) || clonedBoard.getHoles().get(lastIndex).getSeeds() == 0 && lastIndex >5) {
			cpt++;
			lastIndex--;
		}
		
		if (cpt == 6) {
			System.out.println("Starving move : you can't take seeds");
			return true;
		}
		
		return false;
	}
	
	public boolean hasWon() {
		if (this.getBoard().isBeginnerDifficulty()) {
			if (this.getBoard().getSeeds() > 6) {
				return false;
			}
			
			if (this.getPlayerNumber() == 1) {
				if (this.getBoard().getPlayerTwo().getSeedsInArea() == 0) {
					return true;
				}
			}else {
				if (this.getBoard().getPlayerOne().getSeedsInArea() == 0) {
					return true;
				}
			}
		}
		
		return this.granary.getSeeds() >= 25;
	}
	
	private boolean isInArea(int holeIndex) {
		return this.startIndexArea <= holeIndex && this.endIndexArea >= holeIndex;
	}
	
	public int getSeedsInArea() {
		int nbSeedsInArea = 0;
		for(int i=this.startIndexArea; i<=this.endIndexArea; i++) {
			nbSeedsInArea += this.getBoard().getHoles().get(i).getSeeds();
		}
		
		return nbSeedsInArea;
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
	
	public int getScore() {
		return score;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public void addPointToScore() {
		this.score++;
	}

}
