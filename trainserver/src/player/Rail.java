package player;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import train.GameException;

import map.Edge;
import map.Ferry;
import map.Milepost;

public class Rail {
	
	private Map<Milepost, Set<Milepost>> tracks; 
		//all bindings are unordered: if a milepost is in another's set, that one's set contains the milepost
	private Map<Milepost, Set<Track>> allTracks; //same object per game; holds everyone's tracks
	private Map<Milepost, Ferry> allFerries;
	private String pid;
	
	Rail(Map<Milepost, Set<Track>> allTracks, Map<Milepost, Ferry> allFerries, String pid){
		this.allTracks = allTracks;
		tracks = new HashMap<Milepost, Set<Milepost>>();
		this.allFerries = allFerries;
		this.pid = pid;
	}
	
	public Map<Milepost, Set<Milepost>> getRail(){return tracks;}
	
	boolean contains(Milepost m){return tracks.containsKey(m);}
	
	/** Returns whether this rail connects these two mileposts.
	 *  When the two mileposts are not neighbors, the answer is always false.
	 */
	boolean connects(Milepost one, Milepost two){return tracks.get(one).contains(two);}
	
	boolean connectsByFerry(Milepost one, Milepost two){
		return (allFerries.containsKey(one) ? allFerries.get(one).destination.equals(two) : false);
	}
	
	String anyConnects(Milepost one, Milepost two){
		Set<Track> tracks = allTracks.get(one);
		if(tracks == null) return "";
		for(Track t : tracks){
			for(Milepost m : t.dests){
				if(m.equals(two)) return t.pid;
			}
		}
		return "";
	}
	
	private void addTrack(Milepost one, Milepost two) {
		if(!tracks.containsKey(one)){
			tracks.put(one, new HashSet<Milepost>());
		}
		tracks.get(one).add(two);
	}
	
	private void addAllTrack(Milepost one, Milepost two){
		if(!allTracks.containsKey(one)){
			allTracks.put(one, new HashSet<Track>());
		}
		for(Track t : allTracks.get(one)){
			if(t.pid.equals(pid)){
				t.add(two);
				return;
			}
		}
		allTracks.get(one).add(new Track(pid));
		for(Track t : allTracks.get(one)){
			if(t.pid.equals(pid)){
				t.add(two);
				return;
			}
		}
	}
	
	private void removeTrack(Milepost one, Milepost two){
		Set<Milepost> s = tracks.get(one);
		s.remove(two);
	}

	private void removeAllTrack(Milepost one, Milepost two){
		for(Track t : allTracks.get(one)){
			if(t.pid.equals(pid)){
				t.remove(two);
			}
		}
	}
	
	/** Adds the given track to the player's rails, if and only if the track can be legally built.
	 * @param origin: the milepost already attached to the rail; next is where
	 * you build towards.
	 * @return the cost of the build
	 */
	int build(Milepost origin, Milepost next) throws GameException {
		addTrack(origin, next);
		addTrack(next, origin);
		addAllTrack(origin, next);
		addAllTrack(next, origin);
		Edge e = getEdge(origin, next);
		if (e instanceof Ferry){
			Edge back = getEdge(next, origin);
			if(back instanceof Ferry){
				allFerries.put(origin, (Ferry) e);
				allFerries.put(next, (Ferry) back);
			}
			else {
				erase(origin, next);
				throw new GameException("InvalidTrack");
			}
		}
		int cost = e.cost;
		return cost;
	}
	
	private Edge getEdge(Milepost origin, Milepost next){
		for(int i = 0; i < origin.edges.length; i++){
			if(origin.edges[i] != null && origin.edges[i].destination.equals(next)){
				return origin.edges[i];
			}
		}
		return null;
	}
	
	void erase(Milepost one, Milepost two){
		removeTrack(one, two);
		removeTrack(two, one);
		removeAllTrack(one, two);
		removeAllTrack(two, one);
	}
	
	public class Track {
		final String pid;
		Set<Milepost> dests;
		
		Track(String p){
			pid = p;
			dests = new HashSet<Milepost>();
		}
		
		void add(Milepost m){dests.add(m);}
		
		void remove(Milepost m){dests.remove(m);}
	}
}
