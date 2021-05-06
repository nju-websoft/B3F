package dpbf;

import java.util.BitSet;

/**
 * @Author Yuxuan Shi
 */
public class DTree implements Comparable<DTree>{
	int v;
	BitSet X;
	private long bitMap;
	
	DTree(int v, int xs){		
		this.v = v;
		X = new BitSet(xs);
		getloc();
	}
	
	DTree(int v, int xs, int loc){		
		this.v = v;
		X = new BitSet(xs);
		X.set(loc);
		getloc();
	}
	
	DTree(int v, BitSet X){		
		this.v = v;
		this.X = (BitSet) X.clone();
		getloc();
	}
	
	void getloc() {
		bitMap = 0;
		for (int i = X.nextSetBit(0); i >= 0; i = X.nextSetBit(i + 1)) {
			bitMap += X.get(i) ? (1 << i) : 0;
		}
	}
	
	@Override
	public int compareTo(DTree r2) {
		int result = Integer.compare(this.v, r2.v);
		if (result == 0) {
			result = Long.compare(r2.bitMap, this.bitMap);
		}
		return result;
	}

	@Override
	public int hashCode() {
		return v;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return compareTo((DTree)obj) == 0;
	}
}
