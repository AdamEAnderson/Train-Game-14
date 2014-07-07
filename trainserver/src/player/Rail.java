package player;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import map.Edge;
import map.TrainMap;
import map.Milepost;

public class Rail {
	HashMap<Milepost, HashMap<Milepost, Track>> tracks;
	
	Rail(){
		tracks = new HashMap<Milepost, HashMap<Milepost, Track>>();
	}
	
	/** Adds the given track to the player's rails. 
	 * @param origin: the milepost already attached to the rail; next is where
	 * you build towards.
	 * @return the cost of the build
	 */
	int build(Milepost origin, Milepost next) /*throws GameException*/{
		assert tracks.containsKey(origin) && origin.isNeighbor(next); 
			//written as an assertion, never checked
		assert !(tracks.get(origin).containsKey(next)); //track not already built
		Track t = new Track(origin, next);
		HashMap<Milepost, Track> inner = tracks.get(origin);
		inner.put(next, t);
		if(tracks.containsKey(next)){
			HashMap<Milepost, Track> inner2 = tracks.get(next);
			inner2.put(origin, t);
		}else {
			HashMap<Milepost, Track> inner2 = new HashMap<Milepost, Track>();
			inner2.put(origin, t);
			tracks.put(next, inner2);
		}
		int cost = t.getEdge().cost;
		return cost;
	}
}
