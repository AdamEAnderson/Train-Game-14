package map;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/** Serializes and deserializes milepost objects as simple (x,y) (nonJSON) pairs for compactness */
public class MilepostIdShortFormTypeAdapter extends TypeAdapter<MilepostId> {
	
	public MilepostIdShortFormTypeAdapter() {
	}
	
	@Override
	public MilepostId read(final JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
        }
        String xy = reader.nextString();
        String[] parts = xy.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
		return new MilepostId(x, y);
	}

  @Override
  public void write(final JsonWriter writer, final MilepostId value) throws IOException {
      if (value == null) {
          writer.nullValue();
          return;
        }
        String xy = value.x + "," + value.y;
        writer.value(xy);
	}
}