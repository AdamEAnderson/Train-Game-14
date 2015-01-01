package map;

import java.io.IOException;

import train.Game;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/** Serializes and deserializes milepost objects as simple (x,y) (nonJSON) pairs for compactness */
public class MilepostShortFormTypeAdapter extends TypeAdapter<Milepost> {
	private Game gameContext;	/** Used to match to game's milepost objects */
	
	public MilepostShortFormTypeAdapter(Game gameContext) {
		this.gameContext = gameContext;
	}
	
	@Override
	public Milepost read(final JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
        }
        String xy = reader.nextString();
        String[] parts = xy.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
		return gameContext.gameData.getMap().getMilepost(new MilepostId(x, y));
	}

  @Override
  public void write(final JsonWriter writer, final Milepost value) throws IOException {
      if (value == null) {
          writer.nullValue();
          return;
        }
        String xy = value.id.x + "," + value.id.y;
        writer.value(xy);
	}
}