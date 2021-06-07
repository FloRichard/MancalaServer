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
	public boolean isBlocked;
	
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

		System.out.println("Ce joueur sera le joueur numéro " +this.playerNumber);
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
	
	public String getJSONString(Board b) {
		jsonConverter jsonDataBuilder = new jsonConverter(b);
		
		Gson gson = new Gson();
		String json = gson.toJson(jsonDataBuilder); 
		return json;
	}

	@Override
	public void run() {
		while(true) {
			try {
				
				Scanner in = new Scanner(socket.getInputStream());
				PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);
				
				String input = in.next();
				System.out.println(input);
				ClientInputController request = new ClientInputController(input);
				System.out.println("Player "+this.playerNumber+" is playing");
				System.out.println("\tActual board :\n\t"+ this.getJSONString(this.getBoard()));
	    	 	try {
	    	 		if (request.isDifficultyChoice()) {
	    	 			if (request.isBeginnerDifficulty()) {
	    	 				this.getBoard().setBeginnerDifficulty(true);
	    	 			}
	    	 		}
	    	 		
	    	 		if (request.isAMove()){
	    	 			this.LastMove = board.clone();
	    	 			playAMove(request.getHoleIndexPlayed());
	    	 			System.out.println("lastMove "+ getJSONString(this.LastMove));
	    	 			out.println(getJSONString(this.getBoard()));
	    	 			continue;
	    	 		}
	    	 		
	    	 		if (request.isAConfirmation()) {
	    	 			if (request.getConfirmationAction().equals("abort")) {
	    	 				this.board = LastMove.clone();
	    	 				System.out.println("Aborting the move... Returning to"+ getJSONString(this.LastMove));
		    	 			out.println(getJSONString(this.board));	
		    	 			continue;
	    	 			}
	    	 		}
	    	 		
					if (this.hasWon()) {
						this.getBoard().broadcastMsg("Le jouer "+this.playerNumber+" a gagné");
		    	 	}
					
					if (this.getBoard().isNullGame()) {
						this.getBoard().broadcastMsg("Match nul");
					}
					
					this.getBoard().broadcastMsg(getJSONString(this.getBoard()));
					if (this.playerNumber == 1) {
						this.getBoard().getPlayerTwo().isBlocked = false;
						
					}else {
						this.getBoard().getPlayerOne().isBlocked = false;
					}
					this.isBlocked = true;
					
				} catch (UnplayableHoleException | NotYourTurnException e) {
					out.println(e.getMessage());
					System.out.println(e.getMessage());
				}
	    	 	
	    	 
			} catch (IOException e) {
				e.printStackTrace();
			} catch(NoSuchElementException e) {
				System.out.println("Le joueur "+this.playerNumber+" s'est déconnecté. Abandon de la partie");
				this.getBoard().informEnemyOfDisconnection(playerNumber);
				break;
			}
			System.out.println("\tBoard after playing :\n\t"+ this.getJSONString(this.getBoard()));
			
		}
		
	}
	
	private void playAMove(int holeIndex) throws UnplayableHoleException, NotYourTurnException {
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
		
		int index = this.getBoard().distribute(holeIndex);	
		if (moveIsStarvingEnemy(holeIndex)) {
			System.out.println("Move is starving the enemy");
			return;
		}
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
	
	private boolean hasWon() {
		if (!this.getBoard().isBeginnerDifficulty()) {
			return this.getBoard().getSeeds() <= 6 && this.getSeedsInArea() == 0;
		}
		return this.granary.getSeeds() >= 25;
	}
	
	private boolean isInArea(int holeIndex) {
		return this.startIndexArea <= holeIndex && this.endIndexArea >= holeIndex;
	}
	
	private class jsonConverter {
		private ArrayList<Object> seeds;
		private int playerOneGranaryCount;
		private int playerTwoGranaryCount;
		
		public jsonConverter(Board board) {
			seeds = new ArrayList<>();
			for (Hole hole : board.getHoles()) {
				seeds.add(hole.getSeeds());
			}
			playerOneGranaryCount = board.getPlayerOne().getGranary().getSeeds();
			playerTwoGranaryCount = board.getPlayerTwo().getGranary().getSeeds();
		}
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

}
