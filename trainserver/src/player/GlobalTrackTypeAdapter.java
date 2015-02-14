package player;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import map.MilepostId;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/** Serializes and deserializes milepost objects as simple (x,y) (nonJSON) pairs for compactness */
public class GlobalTrackTypeAdapter extends TypeAdapter<GlobalTrack> {

	public GlobalTrackTypeAdapter() {
	}
	
@Override
public GlobalTrack read(final JsonReader reader) throws IOException {
	if (reader.peek() == JsonToken.NULL) {
		reader.nextNull();
		return null;
	}
	
	Map<MilepostPair, Set<String>> tracks = new HashMap<MilepostPair, Set<String>>();
	reader.beginArray();
	while (reader.hasNext()) {                                                                              
		// Read each map entry                
		reader.beginObject();
				
		// read MilepostPair
		/*String pairName = */ reader.nextName();
		reader.beginArray();
		int xFirst = Integer.parseInt(reader.nextString());
		int yFirst = Integer.parseInt(reader.nextString());
		int xSecond = Integer.parseInt(reader.nextString());
		int ySecond = Integer.parseInt(reader.nextString());
		reader.endArray();
		MilepostPair pair = new MilepostPair(new MilepostId(xFirst, yFirst), 
			new MilepostId(xSecond, ySecond));	
		
		/*String pidsName = */ reader.nextName();	// read Set<String> of player names
		Set<String> pids = new HashSet<String>();
		reader.beginArray();
		while (reader.hasNext()) 
			pids.add(reader.nextString());
		if (pids.size() < 1)
			System.out.println("Reading empty player track list " + pids.size());
		tracks.put(pair, pids);
		reader.endArray();
		reader.endObject();
		}
	reader.endArray();
	return new GlobalTrack(tracks);
	}

@Override
public void write(final JsonWriter writer, final GlobalTrack value) throws IOException {
	if (value == null) {
		writer.nullValue();
		return;
	}

	writer.beginArray();
	for (Map.Entry<MilepostPair, Set<String>> entry: value.getTracks().entrySet()) {
		writer.beginObject();
		
		writer.name("pair");	// write the milepost pair
		writer.beginArray();
		writer.value(entry.getKey().first.x);
		writer.value(entry.getKey().first.y);
		writer.value(entry.getKey().second.x);
		writer.value(entry.getKey().second.y);
		writer.endArray();
		
		writer.name("pids");	// write the Set<String> of player names
		writer.beginArray();
		Set<String> pids = entry.getValue();
		if (pids.size() < 1)
			System.out.println("Empty player track list! " + pids.size());
		for (String pid: pids) {
			writer.value(pid);
			}
		writer.endArray();
		writer.endObject();
		}
	writer.endArray();
	}
}