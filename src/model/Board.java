package model;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;


public class Board implements Serializable ,Cloneable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Hole> holes;
	private SimplePlayer playerOne;
	private SimplePlayer playerTwo;
	
	public Board(ArrayList<Hole> holes) {
		this.holes = holes;
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
	
	public int setPlayer(SimplePlayer player) {
		if (playerOne == null) {
			this.playerOne = player;
			this.playerOne.isBlocked = false;
			return 1;
		}else {
			this.playerTwo = player;
			this.playerTwo.isBlocked = true;
			return 2;
		}
	}
	
	public Board clone() {
		Board clonedBoard = null;
        try {
			
			clonedBoard = (Board) super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return clonedBoard;
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
			// TODO Auto-generated catch block
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

}
