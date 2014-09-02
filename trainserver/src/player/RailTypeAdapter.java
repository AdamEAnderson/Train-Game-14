package player;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import map.Milepost;
import map.MilepostId;
import train.Game;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/** Serializes and deserializes milepost objects as simple (x,y) (nonJSON) pairs for compactness */
public class RailTypeAdapter extends TypeAdapter<Rail> {
	private Game gameContext;	/** Used to match to game's milepost objects */
	
	public RailTypeAdapter(Game gameContext) {
		this.gameContext = gameContext;
	}
	
	@Override
	public Rail read(final JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
        }
		Map<Milepost, Set<Milepost>> track = null;
		String pid = null;
		
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("pid")) 
				pid = reader.nextString();
			else if (name.equals("tracks")) 
				track = readTracks(reader);
			else 
				reader.skipValue();
		}
		reader.endObject();
		return new Rail(gameContext.getGlobalRail(), pid, track);
	}

	@Override
	public void write(final JsonWriter writer, final Rail value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
        }
      	writer.beginObject();
		writer.name("pid").value(value.getPid());
		writer.name("tracks");
		writeTracks(writer, value.getRail());
      	writer.endObject();
	}
  
	private Milepost readMilepost(String xy) throws IOException {
        String[] parts = xy.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
		return gameContext.gameData.getMap().getMilepost(new MilepostId(x, y));
	}
	
	private Map<Milepost, Set<Milepost>> readTracks(final JsonReader reader) throws IOException {
		Map<Milepost, Set<Milepost>> tracks = new HashMap<Milepost, Set<Milepost>>();
		reader.beginObject();
		while (reader.hasNext()) {
			String xyKey = reader.nextName();
			Milepost key = readMilepost(xyKey);
			Set<Milepost> value = new HashSet<Milepost>();
			reader.beginArray();
			while (reader.hasNext()) {
				String xyValue = reader.nextString();
				value.add(readMilepost(xyValue));
			}
			tracks.put(key, value);
			reader.endArray();
		}
	     reader.endObject();
	     return tracks;
		
	}
	private void writeTracks(final JsonWriter writer, final Map<Milepost, Set<Milepost>> value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}
	    writer.beginObject();
	    for (Map.Entry<Milepost, Set<Milepost>> entry: value.entrySet()) {
	    	String mp = entry.getKey().x + "," + entry.getKey().y;
	    	writer.name(mp);
	    	writer.beginArray();
	    	for (Milepost m: entry.getValue()) {
		    	mp = m.x + "," + m.y;
		    	writer.value(mp);
	    	}
	    	writer.endArray();
	    }
	    writer.endObject();
	}	
	  

}