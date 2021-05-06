package dpbf;

import graphdeal.ConnectedGraph;

import java.util.*;

public class MSTNode {
	int parent[];
	int tag[];
	int nodeNum;
	int tagNum;
	MSTNode(int nodeNum){
		this.nodeNum = nodeNum;
		
		parent = new int[nodeNum];
		for (int i = 0; i < nodeNum; i++) {
			parent[i] = i;
		}
		tagNum =-1;
		tag = new int[nodeNum];
		for (int i = 0; i < nodeNum; i++) {
			tag[i] = 0;
		}
	}
	
	int find(int x) {
		if (tag[x] != tagNum) {
			tag[x] = tagNum;
			parent[x] = x;
		}
		if (parent[x] != x) {
			parent[x] = find(parent[x]);
		}
		return parent[x];
	}
	
	void union(int x, int y) {
		parent[find(x)] = find(y);
	}
	
	DTreeInfo prim(ConnectedGraph c1, List<EdgeDPBF> edges1){
		Queue<EdgeDPBF> edges = new PriorityQueue<>(edges1);
		tagNum++;
		
		double ans = 0;
		List<EdgeDPBF> treeEdges = new ArrayList<>();
		Set<Integer> nodes = new TreeSet<>();
		while (!edges.isEmpty()) {
			EdgeDPBF it = edges.poll();
			if (find(it.u) != find(it.v)) {
				treeEdges.add(it);
				union(it.u, it.v);
			}
			//try to add it.u to nodes
			if (!nodes.contains(it.u)) {
				ans += c1.getNodeWeight(it.u);
				nodes.add(it.u);
			}
			if (!nodes.contains(it.v)) {
				ans += c1.getNodeWeight(it.v);
				nodes.add(it.v);
			}
		}
		return new DTreeInfo(ans, treeEdges);
	}
}
