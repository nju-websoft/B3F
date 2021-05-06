package dpbf;

public class EdgeDPBF implements Comparable<EdgeDPBF>{
	int u;
	int v;
	double weight;
	EdgeDPBF(int u, int v, double weight){		
		this.u = u;
		this.v = v;
		this.weight = weight;
	}
	
	EdgeDPBF(EdgeDPBF e2){
		this.u = e2.u;
		this.v = e2.v;
		this.weight = e2.weight;
	}
	@Override
	public int compareTo(EdgeDPBF e2) {
		int result = Double.compare(this.weight, e2.weight);
		if (result == 0) {
			result = Integer.compare(e2.u, this.u);
		}
		if (result == 0) {
			result = Integer.compare(e2.v, this.v);
		}
		return result;
	}
}
