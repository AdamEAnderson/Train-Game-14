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
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof MilepostId){
			MilepostId id = (MilepostId) obj;
			return id.x == this.x && id.y == this.y;
		}
		return false;
	}
}