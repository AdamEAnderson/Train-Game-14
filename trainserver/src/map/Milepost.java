package map;

import reference.City;

public class Milepost {
	public final int x;
	public final int y;
	public Edge[] edges; //has length 6: first entry is the NE edge, clockwise 
		//and ending on NW
	public final City city; //null if no city
	public final Type type; 
		//type: only City or MajorCity if hasCity not null
	
	public enum Type {
		CITY, MAJORCITY, NORMAL, BLANK, DESERT, MOUNTAIN, ALPINE, JUNGLE, FOREST
	}

	Milepost(int x, int y, Type type){
		this.edges = null;
		this.city = null;
		this.type = type;
		this.x = x;
		this.y = y;
	}
	
	Milepost(int x, int y, City city, Type type){
		this.edges = null;
		this.city = city;
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
		
	public String toString() {
		StringBuilder builder = new StringBuilder(128);
		builder.append("{" + "\"x\":" + x + "," + "\"y\":" + y + "," + "\"type\":\"" + type.name() + "\",");
		if (city != null)
			builder.append("\"city\":" + city.name + ",\"edges\":[");
		for (Edge edge: edges) {
			if (edge != null)
				builder.append("{" + "\"x\":" + edge.destination.x + "," + "\"y\":" + edge.destination.y + "\"cost\":" + edge.cost + "}");
		}
		builder.append("]}");
		return builder.toString();
	}
	
}
