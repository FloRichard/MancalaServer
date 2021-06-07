package model;

public class Hole  implements Cloneable {
	private int seeds;
	
	public Hole(int seeds) {
		this.seeds = seeds;
	}
	
	public void addSeed() {
		seeds = seeds+1;
	}
	
	public void removeSeeds() {
		seeds = 0 ;
	}
	
	public int retrieveSeeds() {
		int nbRetrievedSeeds = this.seeds; 
		this.seeds = 0;
		return nbRetrievedSeeds;
	}
	
	public int getSeeds() {
		return this.seeds;
	}
	
	public int addSeeds(int seeds) {
		this.seeds += seeds;
		return this.seeds;
	}
	
	public boolean isRetrievable() {
		return this.seeds == 2 || this.seeds == 3;
	}
	
	public Hole clone() {
		Hole clonedHole = null;
        try {
			
			clonedHole = (Hole) super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return clonedHole;
    }
}