package player;

import map.Edge;
import map.Milepost;

public final class Track {
	public final Milepost[] ends;
	
	Track(Milepost one, Milepost two)/*throws GameException*/ {
		assert one != null && two != null; //neither @param may be null
		assert one.isNeighbor(two); //mileposts are neighbors: there's a build-able edge between them
		Milepost[] temp = new Milepost[2];
		Edge[] tempE = new Edge[2];
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
		return null; //or throw a game exception
	}
}
