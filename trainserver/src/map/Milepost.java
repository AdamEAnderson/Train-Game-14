package map;

import reference.City;

public class Milepost {
	public final int x;
	public final int y;
	public Edge[] edges; //has length 6: first entry is the NE edge, clockwise 
		//and ending on NW
	public final City hasCity; //null if no city
	public final Type type; 
		//type: only City or MajorCity if hasCity not null
	
	Milepost(int x, int y, Edge[] edges, City hasCity, Type type){
		this.edges = edges;
		this.hasCity = hasCity;
		this.type = type;
		this.x = x;
		this.y = y;
	}
	
	void updateEdges(Edge[] edges){
		this.edges = edges;
	}
	
	public boolean isNeighbor(Milepost m){
		for(int i = 0; i < 6; i++){
			if(edges[i].destination.equals(m)) return true;
		}
		return false;
	}
	
	public @Override boolean equals(Object obj){
		if(obj instanceof Milepost){
			return (this.x == ((Milepost) obj).x) && 
					this.y == ((Milepost) obj).y;
		}else{
			return false;
		}
	}
		
	enum Type {
		CITY, MAJORCITY, NORMAL, BLANK, DESERT, MOUNTAIN, ALPINE, JUNGLE, FOREST
	}
}
