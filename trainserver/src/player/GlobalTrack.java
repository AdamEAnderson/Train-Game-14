package player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GlobalTrack {

	private Map<MilepostPair, Set<String>> tracks;
	// Compendium of all individual player's track, indexed by milepost pairs(source, dest)
	// each entry contains a list of the players who have connected the mileposts

	public GlobalTrack() {
		tracks = new HashMap<MilepostPair, Set<String>>();
	}
	
	public GlobalTrack(Map<MilepostPair, Set<String>> tracks) {
		this.tracks = tracks;
	}
	
	public Map<MilepostPair, Set<String>> getTracks() {
		return tracks;
	}
}
