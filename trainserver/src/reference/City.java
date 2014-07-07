package reference;

import java.util.List;

public final class City {
	public final String name;
	public final List<Load> loads;
	public final boolean isMajor;
	
	public City(String name, List<Load> loads, boolean isMajor){
		this.name = name;
		this.loads = loads;
		this.isMajor = isMajor;
	}
	
	public boolean hasLoad(Load load){
		return loads.contains(load);
	}
	
	public String getName(){
		return name;
	}
	
	public List<Load> getLoads(){
		return loads;
	}
	
	public boolean isMajorCity(){
		return isMajor;
	}
}
