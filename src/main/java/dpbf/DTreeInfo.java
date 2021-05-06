package dpbf;

import java.util.ArrayList;
import java.util.List;

public class DTreeInfo {
	double cost;
	List<EdgeDPBF> edges;
	DTreeInfo(double cost, List<EdgeDPBF> edges){
		this.cost = cost;
		this.edges = new ArrayList<>();
		if (edges == null) {
			return;
		}
		for (EdgeDPBF it : edges) {
			this.edges.add(new EdgeDPBF(it));
		}
	}
}
