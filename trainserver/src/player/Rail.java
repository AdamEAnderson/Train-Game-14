package player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import train.GameException;
import map.Edge;
import map.Ferry;
import map.Milepost;
import map.MilepostId;

public class Rail {
	
	private Map<MilepostId, Set<MilepostId>> tracks; 
		//all bindings are unordered: if a milepost is in another's set, that one's set contains the milepost
//	private transient Map<Milepost, Set<Track>> allTracks; //same object per game; holds everyone's tracks
	private String pid;
	
	public Rail(String pid){
		this.pid = pid;
		tracks = new HashMap<MilepostId, Set<MilepostId>>();
	}
	/*
	Rail(Map<Milepost, Set<Track>> allTracks, String pid){
		this.allTracks = allTracks;
		tracks = new HashMap<Milepost, Set<Milepost>>();
		this.pid = pid;
	}
	
	Rail(Map<Milepost, Set<Track>> allTracks, String pid, Map<Milepost, Set<Milepost>> tracks){
		this.allTracks = allTracks;
		this.pid = pid;
		this.tracks = tracks;
	}*/
	
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
		return tracks.get(one).contains(two);
	}
	
//	String anyConnects(Milepost one, Milepost two){
//		Set<Track> tracks = allTracks.get(one);
//		if(tracks == null) return "";
//		for(Track t : tracks){
//			for(Milepost m : t.dests){
//				if(m.equals(two)) return t.pid;
//			}
//		}
//		return "";
//	}
	
	private void addTrack(MilepostId one, MilepostId two) {
		if(!tracks.containsKey(one)){
			tracks.put(one, new HashSet<MilepostId>());
		}
		tracks.get(one).add(two);
	}
	
//	private void addAllTrack(Milepost one, Milepost two){
//		if(!allTracks.containsKey(one)){
//			allTracks.put(one, new HashSet<Track>());
//		}
//		for(Track t : allTracks.get(one)){
//			if(t.pid.equals(pid)){
//				t.add(two);
//				return;
//			}
//		}
//		allTracks.get(one).add(new Track(pid));
//		for(Track t : allTracks.get(one)){
//			if(t.pid.equals(pid)){
//				t.add(two);
//				return;
//			}
//		}
//	}
	
	private void removeTrack(MilepostId one, MilepostId two){
		Set<MilepostId> s = tracks.get(one);
		s.remove(two);
	}

//	private void removeAllTrack(Milepost one, Milepost two){
//		for(Track t : allTracks.get(one)){
//			if(t.pid.equals(pid)){
//				t.remove(two);
//			}
//		}
//	}
	
	//moved to Milepost class
//	int getCost(Milepost origin, Milepost next){
//		Edge e = getEdge(origin, next);
//		if(e == null) return -1;
//		return e.cost;
//	}
//	
	/** Adds the given track to the player's rails, if and only if the track can be legally built.
	 * @param origin: the milepost already attached to the rail; next is where
	 * you build towards.
	 * @return the cost of the build
	 */
	int build(Milepost origin, Milepost next) throws GameException {
		if(!origin.isNeighbor(next) || !next.isNeighbor(origin)){
			throw new GameException("InvalidTrack");
		}
		addTrack(origin.getMilepostId(), next.getMilepostId());
		addTrack(next.getMilepostId(), origin.getMilepostId());
//		addAllTrack(origin, next);
//		addAllTrack(next, origin);
		Edge e = origin.getEdge(next);
		if (e instanceof Ferry){
			Edge back = next.getEdge(origin);
			if(!(back instanceof Ferry)){
				erase(origin.getMilepostId(), next.getMilepostId());
				throw new GameException(GameException.INVALID_TRACK);
			}
		}
		int cost = e.cost;
		return cost;
	}
	
	//moved to Milepost class
//	private Edge getEdge(Milepost origin, Milepost next){
//		for(int i = 0; i < origin.edges.length; i++){
//			if(origin.edges[i] != null && origin.edges[i].destination.equals(next)){
//				return origin.edges[i];
//			}
//		}
//		return null;
//	}
	
	void erase(MilepostId one, MilepostId two){
		removeTrack(one, two);
		removeTrack(two, one);
//		removeAllTrack(one, two);
//		removeAllTrack(two, one);
	}
	
	/** After deserializing a game, need to fix up the Rail so the 
	 * globalRail, globalFerry points back to the one set in the Game,
	 * and also so that it is populated.
	 * @param globalRail
	 * @param globalFerries
	 */
//	void fixup(Map<Milepost, Set<Rail.Track>> globalRail) {
//		this.allTracks = globalRail;
//		// Add player's track to the global set, since that isn't persisted
//		for (Milepost src:tracks.keySet()) {
//			for (Milepost dest: tracks.get(src))
//				addAllTrack(src, dest);
//		}
//	}
	
//	public class Track {
//		final String pid;
//		Set<Milepost> dests;
//		
//		Track(String p){
//			pid = p;
//			dests = new HashSet<Milepost>();
//		}
//		
//		void add(Milepost m){dests.add(m);}
//		
//		void remove(Milepost m){dests.remove(m);}
//	}
}
