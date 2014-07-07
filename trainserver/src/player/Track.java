package player;

import map.Edge;
import map.Milepost;

public final class Track {
	public final Milepost[] ends;
	public final Edge[] travel;
	
	Track(Milepost one, Milepost two)/*throws GameException*/ {
		Milepost[] temp = new Milepost[2];
		Edge[] tempE = new Edge[2];
		temp[0] = one;
		temp[1] = two;
		ends = temp;
		for(int i = 0; i < one.edges.length; i++){
			if(one.edges[i].destination.equals(two)){
				tempE[0] = one.edges[i];
			}
		}
		for(int i = 0; i < one.edges.length; i++){
			if(two.edges[i].destination.equals(one)){
				tempE[1] = two.edges[i];
			}
		}
		travel = tempE;
	}
}
