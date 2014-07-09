package map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MilepostId {
	static Gson gson = new GsonBuilder().create();
	public int x;
	public int y;
	
	public MilepostId(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public String toString() {
		return gson.toJson(this);
	}
}