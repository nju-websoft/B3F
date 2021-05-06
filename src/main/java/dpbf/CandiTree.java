package dpbf;

import graphdeal.CompareScore;
import graphdeal.ConnectedGraph;
import graphdeal.Util;

import java.util.*;

public class CandiTree implements CompareScore<CandiTree>, Comparable<CandiTree>{
	int v;
	private BitSet X;
	double weight;
	List<EdgeDPBF> edges;
	private long bitMap;

	CandiTree(int v, int xs, int loc){
		this.v = v;
		X = new BitSet(xs);
		X.set(loc);
		weight = 0;
		edges = new ArrayList<>();
		calcBitMap();
	}

	/**
	 * the canditree for node weighted graph
	 * @param v root
	 * @param xs keyword number
	 * @param weight root's weight
	 * @param loc covered keyword
	 */
	CandiTree(int v, int xs, double weight, int loc){
		this.v = v;
		X = new BitSet(xs);
		X.set(loc);
		this.weight = weight;
		edges = new ArrayList<>();
		calcBitMap();
	}
	
	CandiTree(int v, BitSet X, double weight, List<EdgeDPBF> edges){
		this.v = v;
		this.X = (BitSet) X.clone();
		this.weight = weight;		
		this.edges = new ArrayList<>(edges);
		calcBitMap();
	}

	BitSet getX() { return X;}

	public double getWeight() {
		return weight;
	}

	void calcBitMap() {
		bitMap = 0;
		for (int i = X.nextSetBit(0); i >= 0; i = X.nextSetBit(i + 1)) {
			bitMap += X.get(i) ? (1 << i) : 0;
		}
	}


	@Override
	public int compareTo(CandiTree c2) {
		int result = Double.compare(this.v, c2.v);
		if (result == 0) {
			result = Long.compare(c2.bitMap, this.bitMap);
		}
		return result;
	}

    public double getScore(ConnectedGraph c1) {
		Set<Integer> nodes = new TreeSet<>();
		for (EdgeDPBF it : edges){
			nodes.add(it.u);
			nodes.add(it.v);
		}
		double alpha = Util.ALPHA;
		double beta = 1 - alpha;
		double score = 0D;
		List<Integer> nodeList = new ArrayList<>(nodes);
		for (int i = 0; i < nodeList.size(); i++){
			score += alpha * c1.getNodeWeight(nodeList.get(i));
			for (int j = i + 1; j < nodeList.size(); j++) {
				score += beta * c1.sde(nodeList.get(i), nodeList.get(j));
			}
		}
		return score;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return compareTo((CandiTree) o) == 0;

	}

	@Override
	public int hashCode() {
		return (int) (v*(1<<X.size())+bitMap);
	}

	@Override
	public int compareScore(CandiTree c1) {
		return Double.compare(this.weight, c1.weight);
	}
}
