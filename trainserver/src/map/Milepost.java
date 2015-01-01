package map;

import reference.City;
import train.GameException;

public class Milepost {
	public final MilepostId id;
	public Edge[] edges; //has length 6: first entry is the NE edge, clockwise 
		//and ending on NW
	public final City city; //null if no city
	public final Type type; 
		//type: only City or MajorCity if hasCity not null
	
	public enum Type {
		CITY, MAJORCITY, NORMAL, BLANK, DESERT, MOUNTAIN, ALPINE, JUNGLE, FOREST, CHUNNEL
	}

	Milepost(int x, int y, Type type){
		this.edges = null;
		this.city = null;
		this.type = type;
		id = new MilepostId(x, y);
	}
	
	Milepost(int x, int y, City city, Type type){
		this.edges = null;
		this.city = city;
		this.type = type;
		id = new MilepostId(x, y);
	}
	
	void updateEdges(Edge[] edges) throws GameException{
		this.edges = edges;
	}
	
	public boolean isNeighbor(MilepostId m){
		return getEdge(m) != null;
	}
	
	public boolean isNeighborByFerry(MilepostId m){
		return (getEdge(m) instanceof Ferry);
	}
	
	public @Override boolean equals(Object obj){
		if(obj instanceof Milepost){
			return id.equals(((Milepost)obj).id);
		}else{
			return false;
		}
	}
		
	public boolean isSameCity(Milepost next) {
		return this.city == null ? false : this.city.equals(next.city);
	}
	
	public boolean isMajorCity(){
		return this.type == Type.MAJORCITY;
	}
	
	public MilepostId getMilepostId() {
		return id;
	}
	
	public int getCost(MilepostId m){
		Edge e = getEdge(m);
		if(e == null) return -1;
		return e.cost;
	}
	
	public Edge getEdge(MilepostId m){
		for(int i = 0; i < edges.length; i++){
			if(edges[i] != null && edges[i].destination.id.equals(m)){
				return edges[i];
			}
		}
		return null;
	}
}
