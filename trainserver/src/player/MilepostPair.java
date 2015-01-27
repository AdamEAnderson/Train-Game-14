package player;

import map.MilepostId;

class MilepostPair implements Comparable<MilepostPair> {
	MilepostId first;
	MilepostId second;
	
	public MilepostPair() {
		
	}
	
	public MilepostPair(MilepostId first, MilepostId second) {
		// Order the pairs to make later comparisons faster
		int result = first.compareTo(second);
		if (result < 0) {
			this.first = first;
			this.second = second;
		}
		else if (result > 0) {
			this.first = second;
			this.second = first;
		}
	}

	public boolean equals(MilepostId one, MilepostId two) {
		if (one.compareTo(two) <= 0)
			return one.equals(first) && two.equals(second);
		else
			return two.equals(first) && one.equals(second);
	}
	
	public int compareTo(MilepostPair otherMP) {
		int result = first.compareTo(otherMP.first);
		if (result == 0)
			result = second.compareTo(otherMP.second);
		return result;
	}
};

