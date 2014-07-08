package reference;


public final class Card {
	public final Trip[] trips;
	
	/**Precondition: the cities, loads, and cost must have three elements each,
	 * in corresponding locations of the arrays.
	 */
	public Card(City[] cities, String[] loads, int[] cost){
		Trip[] temp = new Trip[3];
		for(int i = 0; i < 3; i++){
			temp[i] = new Trip(cities[i], loads[i], cost[i]);
		}
		trips = temp;
	}
	
	public final class Trip {
		public final City dest;
		public final String load;
		public final int cost;
		
		public Trip(City dest, String load, int cost){
			this.dest = dest;
			this.load = load;
			this.cost = cost;
		}
	}
}
