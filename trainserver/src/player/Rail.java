package player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import train.GameException;

import map.Edge;
import map.Milepost;

public class Rail {
	
	private Map<Milepost, Set<Milepost>> tracks; 
		//all bindings are reversible: if a milepost is in another's set, that one's set contains the milepost
	private Map<Milepost, Set<Milepost>> allTracks; //same object per game; holds everyone's tracks
	
	Rail(Map<Milepost, Set<Milepost>> all){
		allTracks = all;
		tracks = new HashMap<Milepost, Set<Milepost>>();
	}
	
	public Map<Milepost, Set<Milepost>> getRail(){
		return tracks;
	}
	
	boolean contains(Milepost m){
		return tracks.containsKey(m);
	}
	
	/** Returns whether this rail connects these two mileposts.
	 *  When the two mileposts are not neighbors, the answer is always false.
	 */
	boolean connects(Milepost one, Milepost two){
		return tracks.get(one).contains(two);
	}
	
	boolean anyConnects(Milepost one, Milepost two){
		Set<Milepost> dests = allTracks.get(one);
		return dests != null && allTracks.get(one).contains(two);
	}
	
	/** Adds the given track to the player's rails, if and only if the track can be legally built.
	 * @param origin: the milepost already attached to the rail; next is where
	 * you build towards.
	 * @return the cost of the build
	 */
	int build(Milepost origin, Milepost next) throws GameException {
		if(!tracks.containsKey(origin)){
			tracks.put(origin, new HashSet<Milepost>());
		}
		tracks.get(origin).add(next);
		if(!tracks.containsKey(next)){
			tracks.put(next, new HashSet<Milepost>());
		}
		tracks.get(next).add(origin);
		int cost = getEdge(origin, next).cost;
		return cost;
	}
	
	private Edge getEdge(Milepost origin, Milepost next){
		for(int i = 0; i < origin.edges.length; i++){
			if(origin.edges[i].destination.equals(next)){
				return origin.edges[i];
			}
		}
		return null;
	}
	
	
	/** Erases the track between these neighboring mileposts
	 */
	void erase(Milepost one, Milepost two){
		Set<Milepost> s = tracks.get(one);
		s.remove(two);
		s = tracks.get(two);
		s.remove(one);
	}
}
