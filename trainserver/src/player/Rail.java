package player;
import java.util.HashMap;

import train.GameException;

import map.Milepost;

public class Rail {
	private HashMap<Milepost, HashMap<Milepost, Track>> tracks;
	
	Rail(){
		tracks = new HashMap<Milepost, HashMap<Milepost, Track>>();
	}
	
	/**
	 * @return whether this player has a track between these two mileposts
	 */
	public boolean connects(Milepost one, Milepost two){
		if(tracks.containsKey(one)){
			return tracks.get(one).containsKey(two);
		} return false;
	}
	
	/** Adds the given track to the player's rails. 
	 * @param origin: the milepost already attached to the rail; next is where
	 * you build towards.
	 * @return the cost of the build
	 * Does NOT check that another player did not build this track already
	 */
	int build(Milepost origin, Milepost next) throws GameException {
		if((! tracks.containsKey(origin)) || ! origin.isNeighbor(next)) 
			throw new GameException("InvalidTrack"); 
		if(connects(origin, next)) throw new GameException("InvalidTrack"); 
		if(origin.type == Milepost.Type.BLANK || next.type == Milepost.Type.BLANK) 
			throw new GameException("InvalidTrack");
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
	
	/** Erases the track between these neighboring mileposts
	 */
	void erase(Milepost one, Milepost two){
		HashMap<Milepost, Track> h = tracks.get(one);
		if(h != null) h.remove(two);
		h = tracks.get(two);
		if(h != null) h.remove(one);
	}
}
