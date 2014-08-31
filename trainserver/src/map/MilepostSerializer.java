package map;

import java.lang.reflect.Type;

import train.Game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class MilepostSerializer implements JsonSerializer<Milepost>, JsonDeserializer<Milepost> {
	Game game;
	
	/** Use this constructor if you know you are only going to be serializing (no deserializing)
	 * and you don't have a game context.
	 */
	public MilepostSerializer() {
		game = null;
	}
	/** Use this constructor if you are going to deserialize mileposts */
	public MilepostSerializer(Game game) {
		this.game = game;
	}
	public JsonElement serialize(Milepost src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.toString());
	}		
	public Milepost deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		
		if (game == null)
			throw new JsonParseException("Milepost cannot be deserialized without a game");
		
		String serializededMP = json.getAsJsonPrimitive().getAsString();
		
		Gson gson = new GsonBuilder().create();
		MilepostId data = gson.fromJson(serializededMP, MilepostId.class);
		return game.gameData.getMap().getMilepost(data);
	}
}

