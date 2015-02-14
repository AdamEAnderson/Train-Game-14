package player;

import java.io.IOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/** Serializes and deserializes player's request queue */
public class RequestQueueTypeAdapter extends TypeAdapter<RequestQueue> {

	public RequestQueueTypeAdapter() {
	}
	
	@Override
	public RequestQueue read(final JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
		}
		
		RequestQueue requestQueue = new RequestQueue();
		reader.beginArray();
		while (reader.hasNext()) {
			String request = reader.nextString();
			requestQueue.requestQueue.add(request);
			}
		reader.endArray();
		return requestQueue;
		}
	
	@Override
	public void write(final JsonWriter writer, final RequestQueue value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}
		
		writer.beginArray();
		for (String request: value.requestQueue)
			writer.value(request);
		writer.endArray();
		}
	
}