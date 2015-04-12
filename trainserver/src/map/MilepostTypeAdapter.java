package map;

import java.io.IOException;

import reference.City;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/** Serializes milepost as complete JSON objects.
 * If you need to deserialization, use MilepostShortFormTypeAdaptor, which will match
 * up the milepost objects so they use existing mileposts in the game.
 */
public class MilepostTypeAdapter extends TypeAdapter<Milepost> {

	@Override
	public void write(final JsonWriter writer, final Milepost value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
        }
		writer.beginObject();
		writer.name("x").value(value.id.x);
		writer.name("y").value(value.id.y);
		writer.name("type").value(value.type.toString());
		if (value.city != null) {
			writer.name("city");
			writeCity(writer, value.city);
		}
		writer.name("edges");
		writer.beginArray();
		for (Edge edge: value.edges)
			writeEdge(writer, edge);
		writer.endArray();
		writer.endObject();
	}
	
	private void writeCity(final JsonWriter writer, final City value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
        }
		writer.beginObject();
		writer.name("name").value(value.name);
		writer.name("loads");
		writer.beginArray();
		for (String load: value.loads)
			writer.value(load);
		writer.endArray();
		writer.endObject();
	}

	private void writeEdge(final JsonWriter writer, final Edge value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
        }
		writer.beginObject();
		writer.name("x").value(value.destination.id.x);
		writer.name("y").value(value.destination.id.y);
		writer.name("cost").value(value.cost);
		writer.endObject();
	}
	
	@Override
	public Milepost read(JsonReader arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}