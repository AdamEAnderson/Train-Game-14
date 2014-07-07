package player;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import map.Edge;
import map.Map;
import map.Milepost;

public class Rail {
	HashMap<Milepost, Track> tracks;
	//final Map map;
	
	/*Rail(Map m){
		tracks = new HashMap<Milepost, Track>();
		map = m;
	}*/
	
	Rail(){
		tracks = new HashMap<Milepost, Track>();
	}
	
	/** Adds the given track to the player's rails. 
	 * Incomplete + Error filled. Recursive.
	 * Does NOT charge a cost; does check that the track is a legal build.
	 * @param building: queue of mileposts to be built.
	 */
	void build(Queue<Milepost> building) /*throws GameException*/{
		/*Milepost origin = building.poll();
		// I do not think the nested if-clause does what it should.
		if(tracks.containsKey(origin) || origin.hasCity != null){
			if(origin.hasCity.isMajor || tracks.containsKey(origin)){
				Milepost next = building.peek();
				if(origin.isNeighbor(next)){
					Track t = new Track(origin, next);
					tracks.put(origin, t);
					tracks.put(next, t);
					build(building);
				}
			}	
		}*/
	}
}
