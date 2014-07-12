package map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MilepostId implements Comparable<MilepostId> {
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

	@Override
	public int hashCode()
	{
		return x * y;
	}

	@Override
	public int compareTo(MilepostId o) {
		if (y < o.y)
			return -1;
		else if (y > o.y)
			return 1;
		// y == o.y
		if (x < o.x)
			return -1;
		else if (x > o.x)
			return 1;
		// x == o.x
		return 0;
	}
}