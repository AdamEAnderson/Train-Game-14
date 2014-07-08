package player;

import train.GameException;
import map.Edge;
import map.Milepost;

public final class Track {
	public final Milepost[] ends;
	
	Track(Milepost one, Milepost two) throws GameException {
		if( one == null || two == null) throw new GameException("InvalidTrack"); 
		if(! one.isNeighbor(two)) throw new GameException("InvalidTrack"); 
		Milepost[] temp = new Milepost[2];
		temp[0] = one;
		temp[1] = two;
		ends = temp;
	}
	
	/**
	 * @return the edge from Milepost one to Milepost two
	 */
	Edge getEdge(){
		for(int i = 0; i < ends[0].edges.length; i++){
			if(ends[0].edges[i].destination.equals(ends[1])){
				return ends[0].edges[i];
			}
		}
		return null; //or throw a game exception ?
	}
}
