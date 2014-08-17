package reference;

import java.util.List;

public final class City {
	public final String name;
	public final List<String> loads;
	public final boolean isMajor;
	
	public City(String name, List<String> loads, boolean isMajor){
		this.name = name;
		this.loads = loads;
		this.isMajor = isMajor;
	}
	
	public boolean hasLoad(String load){
		return loads.contains(load);
	}
	
	public String getName(){return name;}
	
	public List<String> getLoads(){return loads;}
	
	public boolean isMajorCity(){return isMajor; }
	
	@Override
	public boolean equals(Object obj){
		return obj instanceof City ? this.name.equals(((City) obj).name) : false;
	}
}
