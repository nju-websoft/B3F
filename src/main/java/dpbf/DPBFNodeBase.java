package dpbf;

import graphdeal.*;

import java.util.*;


/**
 *
 */
public class DPBFNodeBase extends SearchBase {
	GraphDPBF g1;
	int nodeNum;
	int keyNum;	
	BitSet P;
	MSTNode m1;
	MinHeap<CandiTree> QT;
	Map<DTree, DTreeInfo> DT;
	private CandiTree best = null;
	private AnsTreeDPBFNode bestAns = null;

	private static DPBFNodeBase instance = null;

	public static DPBFNodeBase getInstance() {
		if (instance == null) {
			Properties pps = Util.getInitPPS();
			if (!"false".equals(pps.get("INIT"))) {
				instance = new DPBFNodeBase();
			}
		}
		return instance;
	}

	public static void closeInstance() {
		instance = null;
	}
	@Override
	public AnsTree getAnsTree(){
		return bestAns;
	}

	@Override
	public JsonTree JsonGenerate(){
		JsonTree js = new JsonTree();
		js.buildTree(bestAns, "DPBF");
		return js;
	}

	void dpbfDeal(ConnectedGraph c1) {
		nodeNum = c1.getNodeNum();
		keyNum = g1.getKeywordNum();
		//record P in the form of BitSet 
		P = new BitSet(keyNum);
		P.flip(0, keyNum);
		
		m1 = new MSTNode(nodeNum);
		//QT = new MinHeap<>(1<<keyNum);
		QT = new MinHeap<>();
		DT = new HashMap<>();

		//line 3-5
		for (int i = 0; i < keyNum; i++) {
			for (int it : c1.getKeynode(g1.keywordList.get(i))) {
				QT.push(new CandiTree(it, keyNum,c1.getNodeWeight(it), i));
			}
		}
		//line 6
		while (!QT.isEmpty()) {
			if (isTimeOut()) {
				break;
			}
			CandiTree ct = QT.poll();			
			if (DT.containsKey(new DTree(ct.v, ct.getX()))) {
				continue;
			}
			DT.put(new DTree(ct.v, ct.getX()), new DTreeInfo(ct.weight, ct.edges));
			//line 7
			if (ct.getX().equals(P)) {
				if (best == null || best.getWeight() > ct.getWeight()) {
					best = ct;
					bestAns = new AnsTreeDPBFNode(c1, best);
				}
				return;
			}
			int v = ct.v;
			//line 9
			for (HopVDE it : c1.getEdges(v)) {
				DTreeInfo dti = m1.prim(c1, edgesMerge(ct.edges, new EdgeDPBF(v, it.v, it.getDis())));
				//line 10
				QT.push(new CandiTree(it.v, ct.getX(), dti.cost, dti.edges));
			}
			
			BitSet Xbar = (BitSet)P.clone();
			Xbar.xor(ct.getX());
			findSubset(c1, ct, ct.getX(), Xbar, 0);
		}
	}
	
	//line 13-17
	void findSubset(ConnectedGraph c1, CandiTree ct, BitSet X, BitSet Xbar, int loc) {
		if (Xbar.cardinality() > 0) {
			DTree newdt = new DTree(ct.v, Xbar);
			if (DT.containsKey(newdt)) {
				X.xor(Xbar);
				DTree candt = new DTree(ct.v, X);
				if (!DT.containsKey(candt)) {
					DTreeInfo dti = m1.prim(c1, edgesMerge(ct.edges, DT.get(newdt).edges));
					CandiTree nct = new CandiTree(ct.v, X, dti.cost, dti.edges);
					QT.push(nct);

					//record candidate result
					if (nct.getX().equals(P)) {
						if (best == null || best.getWeight() > nct.getWeight()) {
							best = nct;
							bestAns = new AnsTreeDPBFNode(c1, best);
						}
						return;
					}
				}
				X.xor(Xbar);
			}
		}
		for (int i = Xbar.nextSetBit(loc); i >= 0; i = Xbar.nextSetBit(i + 1)) {
			Xbar.flip(i);
			findSubset(c1, ct, X, Xbar, i + 1);
			Xbar.flip(i);
		}
	}	
	
	List<EdgeDPBF> edgesMerge(List<EdgeDPBF> edges1, EdgeDPBF edge2) {
		List<EdgeDPBF> edges = new ArrayList<>(edges1);
		edges.add(edge2);
		return edges;
	}
	
	List<EdgeDPBF> edgesMerge(List<EdgeDPBF> edges1, List<EdgeDPBF> edges2) {
		List<EdgeDPBF> edges = new ArrayList<>(edges1);
		edges.addAll(edges2);
		return edges;
	}

	void search() throws Exception {
		try {
			best = null;
			bestAns = null;
			setRepeatFlag();
			for (ConnectedGraph c1 : g1.cg) {
				if (!c1.keyContains(g1.keywordList)) {
					continue;
				}
				dpbfDeal(c1);
			}

			if (bestAns == null) {
				throw new Exception("find no answer");
			}
			bestAns.calcScore(Util.ALPHA);
		} catch (OutOfMemoryError e) {
			DT.clear();
			QT.clear();
			Runtime.getRuntime().gc();
		}
		//bestAns.setScore(bestAns.getWeight());
		//bestAns.printTree();
	}

	@Override
	public void search(GraphBase gg, List<String> keywordList0, Map<String, List<Integer>> mp) throws Exception {
		startTime = System.currentTimeMillis();
		g1 = (GraphDPBF)gg;
		g1.givenQueryWord(keywordList0, mp);
		search();
	}

	@Override
	public void search(GraphBase gg, List<String> keywordList0) throws Exception {
		startTime = System.currentTimeMillis();
		g1 = (GraphDPBF)gg;
		g1.givenQueryWord(keywordList0);
		search();
	}
}
