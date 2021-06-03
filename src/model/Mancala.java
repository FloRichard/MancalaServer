package model;

public class Mancala {
	private Board board;
	private Action action;
	
	public Mancala(Board board, Action action) {
		this.board = board;
		this.action = action;
	}
	
	public Board getBoard() {
		return board;
	}
	public void setBoard(Board board) {
		this.board = board;
	}
	
	public Action getAction() {
		return action;
	}
	public void setAction(Action action) {
		this.action = action;
	}
}
