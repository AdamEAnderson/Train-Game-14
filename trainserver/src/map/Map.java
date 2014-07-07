package map;

import java.util.HashMap;

public final class Map {
	private final HashMap<Milepost, Edge> edges;
	private final HashMap<Integer, HashMap<Integer, Milepost>> vertices;
	
	public Map(String data){
		edges = new HashMap<Milepost, Edge>();
		vertices = new HashMap<Integer, HashMap<Integer, Milepost>>();
	}
}
