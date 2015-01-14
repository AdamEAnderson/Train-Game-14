package player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import train.GameException;
import map.Edge;
import map.Milepost;
import map.MilepostId;

public class Rail {
	
	private Map<MilepostId, Set<MilepostId>> tracks; 
		//all bindings are unordered: if a milepost is in another's set, that one's set contains the milepost
	private String pid;
	
	public Rail(String pid){
		this.pid = pid;
		tracks = new HashMap<MilepostId, Set<MilepostId>>();
	}
	
	public Rail(String pid, Map<MilepostId, Set<MilepostId>> tracks){
		this.pid = pid;
		this.tracks = tracks;
	}
	
	public Map<MilepostId, Set<MilepostId>> getRail(){
		return tracks;
	}
	
	String getPid() {
		return pid;
	}
	
	/** True if the player has built to this milepost */
	boolean contains(MilepostId m){
		return tracks.containsKey(m);
	}
	
	/** Returns whether this rail connects these two mileposts.
	 *  When the two mileposts are not neighbors, the answer is always false.
	 */
	boolean connects(MilepostId one, MilepostId two){
		if(tracks.containsKey(one))
			return tracks.get(one).contains(two);
		return false;
	}
	
	private void addTrack(MilepostId one, MilepostId two) {
		if(!tracks.containsKey(one)){
			tracks.put(one, new HashSet<MilepostId>());
		}
		tracks.get(one).add(two);
	}
	
	private void removeTrack(MilepostId one, MilepostId two){
		Set<MilepostId> s = tracks.get(one);
		s.remove(two);
	}

	/** Adds the given track to the player's rails, if and only if the track can be legally built.
	 * @param origin: the milepost already attached to the rail; next is where
	 * you build towards.
	 * @return the cost of the build
	 */
	int build(Milepost origin, Milepost next) throws GameException {
		if(!origin.isNeighbor(next.id) || !next.isNeighbor(origin.id)){
			throw new GameException("InvalidTrack");
		}
		addTrack(origin.getMilepostId(), next.getMilepostId());
		addTrack(next.getMilepostId(), origin.getMilepostId());
		Edge e = origin.getEdge(next.id);
		int cost = e.cost;
		return cost;
	}
	
	void erase(MilepostId one, MilepostId two){
		removeTrack(one, two);
		removeTrack(two, one);
	}
	
}
