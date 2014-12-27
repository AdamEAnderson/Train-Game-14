package player;

import java.util.HashMap;
import java.util.Map;

import map.Milepost;
import map.MilepostId;
import train.GameException;

public class GlobalRail {
	private Map<String, Rail> rails; //maps pid to their track
	private String gid;
	
	public GlobalRail(String gid){
		this.gid = gid;
		rails = new HashMap<String, Rail>();
	}
	
	public void join(String pid){
		rails.put(pid, new Rail(pid));
	}
	
	//returns -1 if the build is invalid
	public int checkBuild(String pid, Milepost[] mps) throws GameException{
		int cost = 0;
		Milepost fst = mps[0];
		if(!(contains(pid, fst.getMilepostId()) || fst.isMajorCity()))
			return -1;
		for(int i = 1; i < mps.length; i++){
			Milepost snd = mps[i];
			if(!fst.isNeighbor(snd)) 
				return -1;
			if(anyConnects(fst.getMilepostId(), snd.getMilepostId()))
				return -1;
			cost += fst.getCost(snd);
			fst = snd;
		}
		return cost;
	}
	
	public void build(String pid, Milepost[] mps) throws GameException{
		if(!rails.containsKey(pid))
			throw new GameException("PlayerNotFound");
		Rail r = rails.get(pid);
		if(checkBuild(pid, mps) == -1){
			throw new GameException("InvalidTrack");
		}
		Milepost fst = mps[0];
		for(int i = 1; i < mps.length; i++){
			Milepost snd = mps[i];
			r.build(fst, snd);
			fst = snd;
		}
	}
	
	public boolean testMove(String rid, MilepostId[] mps) throws GameException{
		if(!rails.containsKey(rid)) return false;
		MilepostId fst = mps[0];
		for(int i = 1; i < mps.length; i++){
			MilepostId snd = mps[i];
			if(!connects(rid, fst, snd)) return false;
		}
		return true;
	}
	
	public boolean contains(String pid, MilepostId m) throws GameException{
		if(!rails.containsKey(pid)){
			throw new GameException("PlayerNotFound");
		}
		return rails.get(pid).contains(m);
	}
	
	public boolean connects(String pid, MilepostId one, MilepostId two) throws GameException{
		if(!rails.containsKey(pid)){
			throw new GameException("PlayerNotFound");
		}
		return rails.get(pid).connects(one, two);
	}
	
	public boolean anyConnects(MilepostId one, MilepostId two){
		for(String pid : rails.keySet()){
			try {
				return connects(pid, one, two);
			} catch (GameException e) {		} //the exception is thrown when the pid is not identified
		}
		return false;
	}
	
	/** Return the player who build this pair of mileposts, or null if none. 
	 * 
	 * @param one
	 * @param two
	 * @return
	 */
	public String getPlayer(MilepostId one, MilepostId two){
		for(String pid : rails.keySet()){
			try{
				if(connects(pid, one, two)) return pid;
			}catch(GameException e) { }
		}
		return null;
	}

	public Rail getRail(String pid){
		return rails.get(pid);
	}
}