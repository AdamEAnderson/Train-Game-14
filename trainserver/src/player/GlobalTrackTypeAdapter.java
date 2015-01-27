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
	reader.beginObject();
	while (reader.hasNext()) {                                                                              
		// Read each map entry                
				
		// read MilepostPair
		String pairString = reader.nextName();
		MilepostPair pair = readMilepostPair(pairString);
		
		// read Set<String> of player names
		Set<String> pids = new HashSet<String>();
		reader.beginArray();
		while (reader.hasNext()) {
			String pid = reader.nextString();
			pids.add(pid);
		}
		if (pids.size() < 1)
			System.out.println("Reading empty player track list " + pids.size());
		tracks.put(pair, pids);
		reader.endArray();
		}
	reader.endObject();
	return new GlobalTrack(tracks);
	}

@Override
public void write(final JsonWriter writer, final GlobalTrack value) throws IOException {
	if (value == null) {
		writer.nullValue();
		return;
	}

	writer.beginObject();
	for (Map.Entry<MilepostPair, Set<String>> entry: value.getTracks().entrySet()) {
		// write the milepost pair
		String first = entry.getKey().first.x + "," + entry.getKey().first.y;
		String second = entry.getKey().second.x + "," + entry.getKey().second.y;
		String pairString = first + "," + second;
		writer.name(pairString);
		
		// write the Set<String> of player names
		writer.beginArray();
		Set<String> pids = entry.getValue();
		if (pids.size() < 1)
			System.out.println("Empty player track list! " + pids.size());
		for (String pid: pids) {
			writer.value(pid);
			}
		writer.endArray();
		}
	writer.endObject();
	}

private MilepostPair readMilepostPair(String xy) throws IOException {
	String[] parts = xy.split(",");
	int xFirst = Integer.parseInt(parts[0]);
	int yFirst = Integer.parseInt(parts[1]);
	MilepostId first = new MilepostId(xFirst, yFirst);
	int xSecond = Integer.parseInt(parts[2]);
	int ySecond = Integer.parseInt(parts[3]);
	return new MilepostPair(new MilepostId(xFirst, yFirst), new MilepostId(xSecond, ySecond));
	}


}