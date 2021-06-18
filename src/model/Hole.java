package model;


/**
 * A class that represent a hole in Mancala Game. Holes are contained in the board.
 * @author Florian RICHARD
 * @author Julien MONTEIL
 *
 */
public class Hole  implements Cloneable {
	/**
	 * Number of seeds in the hole.
	 */
	private int seeds;
	
	public Hole(int seeds) {
		this.seeds = seeds;
	}
	
	/**
	 * Add a seed to the hole.
	 */
	public void addSeed() {
		seeds = seeds+1;
	}
	
	/**
	 * Remove all the seeds in the hole and return the number of seeds
	 * that was in.
	 * @return the number of seeds in the hole before setting it to 0.
	 */
	public int retrieveSeeds() {
		int nbRetrievedSeeds = this.seeds; 
		this.seeds = 0;
		return nbRetrievedSeeds;
	}
	
	public void setSeeds(int seeds) {
		this.seeds = seeds;
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