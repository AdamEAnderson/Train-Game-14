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
public class RailTypeAdapter extends TypeAdapter<Rail> {

	public RailTypeAdapter() {
	}
	
@Override
public Rail read(final JsonReader reader) throws IOException {
	if (reader.peek() == JsonToken.NULL) {
		reader.nextNull();
		return null;
	}
	
	Map<MilepostId, Set<MilepostId>> track = null;
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
	return new Rail(pid, track);
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

private MilepostId readMilepost(String xy) throws IOException {
	String[] parts = xy.split(",");
	int x = Integer.parseInt(parts[0]);
	int y = Integer.parseInt(parts[1]);
	return new MilepostId(x, y);
	}

private Map<MilepostId, Set<MilepostId>> readTracks(final JsonReader reader) throws IOException {
	Map<MilepostId, Set<MilepostId>> tracks = new HashMap<MilepostId, Set<MilepostId>>();
	reader.beginObject();
	while (reader.hasNext()) {
		String xyKey = reader.nextName();
		MilepostId key = readMilepost(xyKey);
		Set<MilepostId> value = new HashSet<MilepostId>();
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

private void writeTracks(final JsonWriter writer, final Map<MilepostId, Set<MilepostId>> value) throws IOException {
	if (value == null) {
		writer.nullValue();
		return;
	}
	writer.beginObject();
	for (Map.Entry<MilepostId, Set<MilepostId>> entry: value.entrySet()) {
		String mp = entry.getKey().x + "," + entry.getKey().y;
		writer.name(mp);
		writer.beginArray();
		for (MilepostId m: entry.getValue()) {
			mp = m.x + "," + m.y;
			writer.value(mp);
			}
		writer.endArray();
		}
	writer.endObject();
	}
}