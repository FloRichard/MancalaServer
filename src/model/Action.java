package model;

public class Action {
	private Player player;
	private int selectedHoleIndex;
	private Hole hole;
	
	public Action(Player player, int selectedHoleIndex, Hole hole) {
		this.player = player;
		this.selectedHoleIndex = selectedHoleIndex;
		this.hole = hole;
	}
	
	public Player getPlayer() {
		return player;
	}
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public int getSelectedHoleIndex() {
		return selectedHoleIndex;
	}
	public void setSelectedHoleIndex(int selectedHoleIndex) {
		this.selectedHoleIndex = selectedHoleIndex;
	}
	
	public Hole getHole() {
		return hole;
	}
	public void setHole(Hole hole) {
		this.hole = hole;
	}
}
