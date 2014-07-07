package reference;

public final class Load {
	public final String name;
	
	public Load(String name){
		this.name = name;
	}
	
	public @Override boolean equals(Object obj){
		if(obj instanceof Load && 
				((Load) obj).name == this.name) return true;
		return false;
	}
}
