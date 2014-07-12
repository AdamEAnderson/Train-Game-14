package map;


public final class Edge {
	public final Milepost source;
	public final Milepost destination;
	public final int cost;

	Edge(Milepost source, Milepost destination, boolean river, boolean lake){
		this.source = source;
		this.destination = destination;
		int temp;
		
		if(destination.type == Milepost.Type.ALPINE){
			temp = 5;
		} else if(destination.type == Milepost.Type.MOUNTAIN){
			temp = 2;
		} else if(destination.type == Milepost.Type.BLANK){
			temp = Integer.MAX_VALUE;
		} else if(destination.type == Milepost.Type.DESERT){
			temp = 1;
		} else if(destination.type == Milepost.Type.FOREST){
			temp = 2;
		} else if(destination.type == Milepost.Type.JUNGLE){
			temp = 3;
		} else if(destination.type == Milepost.Type.CITY){
			temp = 3;
		} else if(destination.type == Milepost.Type.MAJORCITY){
			temp = 1;
		} else temp = 1;
		
		if(river) temp += 2;
		if(lake) temp += 3;
		
		if(source.type == Milepost.Type.BLANK) temp = Integer.MAX_VALUE;
		
		cost = temp;
	}
	/*
	public @Override boolean equals(Object obj){
		if(obj instanceof Edge){
			Edge temp = (Edge) obj;
			if(temp.source)
		}
		return false;
	}*/

	// Serialize as a set of json-encoded milepostIds
	public String toString() {
		MilepostId sourceId = new MilepostId(source.x, source.y);
		MilepostId destinationId = new MilepostId(destination.x, destination.y);
		System.out.println("Edge " + "{" + "\"source\":" + sourceId.toString() + "," + "\"destination\":" + destinationId.toString() + "}");
		return "{" + "\"source\":" + sourceId.toString() + "," + "\"destination\":" + destinationId.toString() + "}";
	}
	
}
