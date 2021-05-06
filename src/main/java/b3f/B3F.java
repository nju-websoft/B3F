package b3f;

import graphdeal.*;

import java.util.*;

/**
 * @Author Yuxuan Shi
 * @Date 11/6/2019
 * @Time 10:57 AM
 */
public class B3F extends SearchBase {
    static final int MAXKEYNUM = 32;
    int keywordNum = -1;
    //record all one subset ans
    int []keyBit;
    double []keyWeightBit;
    int allOne;
    Queue<Path> pathU;	//generate path list
    ArrayList<ArrayList<ArrayList<Path>>> pathP;	//used path
    double minWT;	//min wt
    int []nodeCount;	//count the times of a node appear
    AnsTreeB3F xtop;	//record top result
    int[] smallList;
    GraphB3F bag;
    //merged paths
    List<List<Map.Entry<Integer, Integer>>>mergeEdges;

    private static B3F instance = null;
    public static B3F getInstance() {
        if (instance == null) {
            return new B3F();
        }
        if (instance == null) {
            Properties pps = Util.getInitPPS();
            if (pps.get("INIT").equals("true")) {
                instance = new B3F();
            }
        }
        return instance;
    }

    @Override
    public AnsTree getAnsTree(){
        return xtop;
    }
    @Override
    public JsonTree JsonGenerate(){
        JsonTree js = new JsonTree();
        js.buildTree(xtop, "BBA");
        return js;
    }

    public static void closeInstance() {
        instance = null;
    }

    public B3F(){
    }

    //try update possible key
    void updateKeyBit(int bitMap, int pos, int k){
        //only none-zero bit need to be covered
        if (keyBit[bitMap] == 0) {
            keyBit[bitMap] = k;
            for (int i = pos; i < keywordNum; i++) {
                if ((bitMap & (1<<i)) != 0) {
                    updateKeyBit(bitMap ^ (1 << i), i + 1, k);
                }
            }
        }
    }

    /**
     * min set cover record in keyBit, min weight set cover in keyWeightBit
     * @param c1 connected graph
     */
    void getMSC(ConnectedGraph c1) {
        List<Integer> newSet = new ArrayList<>();
        //all set covered with one node
        List<Integer> oneSetBit = new ArrayList<>();

        allOne = 0;
        for (int i = 0; i < keywordNum; i++) {
            allOne = allOne + (1 << i);
        }

        //get all set covered by one node
        keyBit = new int[1 << keywordNum];
        Arrays.fill(keyBit, 0);
        for (int v = 0; v < c1.getNodeNum(); v++) {
            int bitMap = bag.bitList[v];
            if (bitMap != 0 && keyBit[bitMap] == 0) {
                oneSetBit.add(bitMap);
                updateKeyBit(bitMap, 0, 1);
            }
        }
        List<Integer> prevSet = new ArrayList<>(oneSetBit);

        //use prevSet to get newSet
        int iteCnt = 2;
        while (prevSet.size() != 0) {
            for (Integer i : prevSet) {
                for (Integer j : oneSetBit) {
                    int bitMap2 = i | j;
                    if (bitMap2 != 0 && keyBit[bitMap2] == 0) {
                        newSet.add(bitMap2);
                        updateKeyBit(bitMap2, 0, iteCnt);
                    }
                }
            }
            prevSet.clear();
            prevSet.addAll(newSet);
            newSet.clear();
            iteCnt++;
        }
        keyBit[0] = 0;
        oneSetBit.clear();

        //generate weighted set cover
        //map keyword bit to a vertex
        Map<Integer, Integer> bitV = new HashMap<>();
        for (int v = 0; v < c1.getNodeNum(); v++) {
            int bitMap = bag.bitList[v];
            if (bitMap == 0) {
                continue;
            }
            if (!bitV.containsKey(bitMap) || c1.getNodeWeight(bitV.get(bitMap)) > c1.getNodeWeight(v)) {
                bitV.put(bitMap, v);
            }
        }
        //sort candidate vertex according to node weight
        List<Integer> candiV = new ArrayList<>(bitV.values());
        Collections.sort(candiV, Comparator.comparingDouble(c1::getNodeWeight));
        //generate all bit V
        bitV.clear();
        for (int v : candiV) {
            subBitVFind(c1, bitV, v, bag.bitList[v]);
        }

        keyWeightBit = new double[1 << keywordNum];
        Arrays.fill(keyWeightBit, -1D);
        keyWeightBit[0] = 0D;
        for (Map.Entry<Integer, Integer> it: bitV.entrySet()) {
            int bitMap = it.getKey();
            int v = it.getValue();
            if (bitMap == 0) {
                continue;
            }
            for (int i = (1 << keywordNum) - 1; i >= bitMap; i--) {
                if (keyWeightBit[i - bitMap] >= 0 &&
                        (keyWeightBit[i] < 0 || keyWeightBit[i] > keyWeightBit[i - bitMap] + c1.getNodeWeight(v))) {
                    keyWeightBit[i] = keyWeightBit[i - bitMap] + c1.getNodeWeight(v);
                }
            }
        }

        /*for (Map.Entry<Integer, Integer> it : bitV.entrySet()) {
            int bitMap = it.getKey();
            for (int i =0; i < keywordNum; i++) {
                if ((bitMap & (1 << i)) != 0) {
                    System.out.print(i + " ");
                }
            }
            System.out.println(":" + it.getValue() + " " + c1.getNodeWeight(it.getValue()));
        }
        for (int v =0; v < keyWeightBit.length; v++) {
            for (int i =0; i < keywordNum; i++) {
                if ((v & (1 << i)) != 0) {
                    System.out.print(i + " ");
                }
            }
            System.out.println(":" + keyWeightBit[v]);
        }*/

    }

    /**
     * find all candidate bitset vertex
     * @param c1 current graph
     * @param bitV the bit vertex map
     * @param v candidate vertex
     * @param bitMap the map of the vertex
     */
    void subBitVFind(ConnectedGraph c1, Map<Integer, Integer> bitV, int v, int bitMap) {
        if (bitMap == 0) {
            return;
        }
        if (bitV.containsKey(bitMap) && c1.getNodeWeight(bitV.get(bitMap)) < c1.getNodeWeight(v)) {
            return;
        }
        bitV.put(bitMap, v);
        for (int i = 0; i < keywordNum; i++) {
            if ((bitMap & (1 << i)) != 0) {
                subBitVFind(c1, bitV, v, bitMap ^ (1 << i));
            }
        }
    }

    /**
     * get minWT
     * @param c1 current graph
     */
    void bbaWT(ConnectedGraph c1) {
        minWT = -1;
        for (int i = 0; i < c1.getNodeNum(); i++) {
            if (c1.getEdges(i).size() > 0 && bag.bitList[i] != 0 && (c1.getNodeWeight(i) < minWT || minWT < 0)) {
                minWT = c1.getNodeWeight(i);
            }
        }
    }


    /**
     * update node v's contribution to newPath
     * @param c1 current graph
     * @param newPath
     */
    void pathUpdate(ConnectedGraph c1, Path newPath) {
        int v = newPath.node;
        newPath.sigmaWT = newPath.sigmaWT + c1.getNodeWeight(v);

        Path tmpp = newPath.former;
        while (tmpp != null) {
            newPath.sigmaSD = newPath.sigmaSD + c1.sde(tmpp.node, v);
            tmpp = tmpp.former;
        }

        newPath.bitM = newPath.bitM | bag.bitList[v];

        int m = keyBit[(~newPath.bitM)&allOne];
        double weight = keyWeightBit[(~newPath.bitM)&allOne];
        newPath.score = Util.ALPHA * (newPath.sigmaWT + weight)
                + Util.BETA * (1 + m / (1 + 2 * Math.floor(((double) newPath.length - 1) / 2)))
                * newPath.sigmaSD;
    }

    boolean biContain(int x, int y) {
        if ((x | y) == x ||(x | y) == y) {
            return true;
        }
        return false;
    }

    //merge paths rooted at v
    void merge(ConnectedGraph c1, Path graph, int v, int number) {
        //find a new result
        if ((graph.bitM & allOne) == allOne) {
            graph.score = graph.getWeight();
            //get a better result
            if (xtop == null || graph.score < xtop.getScore()) {
                xtop = new AnsTreeB3F(c1, v, mergeEdges);
                assert c1.checkTreeCover(xtop, bag.keywordList);
                //System.out.println("find one "+ xtop.getScore());
            }
            return;
        }

        if (number < keywordNum) {
            if (xtop != null &&
                    xtop.getScore()<= graph.getWeight() * Util.B3FACC) {
                return;
            }
            if ((graph.bitM & (1 << smallList[number])) != 0) {
                merge(c1, graph, v, number + 1);
            } else {
                int key = smallList[number];
                for (int i = 0; i < pathP.get(v).get(key).size(); i++) {
                    Path tmpPath = pathP.get(v).get(key).get(i);
                    //a uncovered path
                    if (!biContain(tmpPath.bitM,graph.bitM)) {
                        Path newGraph = graph;
                        //add nodes to graph
                        Path nodePath = tmpPath;
                        while (nodePath != null) {
                            if (nodeCount[nodePath.node] == 0) {
                                newGraph = new Path(newGraph, nodePath.node);
                                pathUpdate(c1, newGraph);
                                //the candidate graph is worse than
                                if (xtop != null && xtop.getScore() <= newGraph.score * Util.B3FACC) {
                                    break;
                                }
                            }
                            nodePath = nodePath.former;
                        }
                        //lower bound is already too big
                        if (xtop != null && xtop.getScore() <= newGraph.score * Util.B3FACC) {
                            continue;
                        }
                        //add paths
                        List<Map.Entry<Integer, Integer>> mergeEdge = new ArrayList<>();
                        while (tmpPath != null) {
                            nodeCount[tmpPath.node]++;
                            //record edges
                            if (tmpPath.former != null) {
                                mergeEdge.add(new AbstractMap.SimpleEntry<>(tmpPath.node, tmpPath.former.node));
                            }
                            tmpPath = tmpPath.former;
                        }
                        mergeEdges.add(mergeEdge);
                        merge(c1, newGraph, v, number + 1);

                        tmpPath = pathP.get(v).get(key).get(i);
                        while (tmpPath != null) {
                            nodeCount[tmpPath.node]--;
                            tmpPath = tmpPath.former;
                        }
                        mergeEdges.remove(mergeEdges.size() - 1);
                    }
                    if (xtop != null && xtop.getScore() < graph.getWeight() * Util.B3FACC) {
                        return;
                    }
                }

            }
        }

    }

    //Best-First Branch-and-bound Algorithm
    void bbaMain(ConnectedGraph c1) {
        pathU = new PriorityQueue<>();
        nodeCount = new int[c1.getNodeNum()];
        smallList = new int[keywordNum];
        mergeEdges = new ArrayList<>();

        //line 3-6
        Path newPath;
        for (int i = 0; i < c1.getNodeNum(); i++) {
            int bitMap = bag.bitList[i];
            if (bitMap != 0) {
                //a new start path
                newPath = new Path(bitMap, i);
                pathUpdate(c1, newPath);
                pathU.add(newPath);
            }
        }
        //line 7-8
        pathP = new ArrayList<>();
        //all paths end at i
        for (int i = 0; i < c1.getNodeNum(); i++) {
            pathP.add(new ArrayList<>());
            //all paths starting from keyword k
            for (int k = 0; k < keywordNum; k++) {
                pathP.get(i).add(new ArrayList<>());
            }
        }

        Path ptop;
        //int pruned1 = 0, pruned2 = 0;
        int itimes = 0;
        while (!pathU.isEmpty()) {
            itimes++;
            if (itimes %100000 == 0) {
                if (isTimeOut()) {
                    break;
                }
            }
            //line 10-12
            //the smallest path
            ptop = pathU.poll();
            if (ptop == null || (xtop != null && xtop.getScore() <= ptop.score * Util.B3FACC)) {
                //pruned2 += pathU.size();
                break;
            }
            //get ptop's end vertex
            int v = ptop.node;
            /*if (xtop != null) {
                System.out.println(itimes + " " + ptop.score  + " " + xtop.getScore() + " " + itimes);
            }*/

            //line 15-19
            Path tmpPath = ptop;
            List<Map.Entry<Integer, Integer>> mergeEdge = new ArrayList<>();
            while (tmpPath != null) {
                nodeCount[tmpPath.node]++;
                //record edges
                if (tmpPath.former != null) {
                    mergeEdge.add(new AbstractMap.SimpleEntry<>(tmpPath.node, tmpPath.former.node));
                }
                tmpPath = tmpPath.former;
            }
            mergeEdges.add(mergeEdge);
            //smallList[firstloc~keywordNum] are uncovered keys
            int firstloc = 0, secondloc = keywordNum - 1;
            for (int i = 0; i < keywordNum; i++) {
                if ((ptop.bitM & (1 << i)) != 0) {
                    smallList[firstloc++] = i;
                } else {
                    smallList[secondloc--] = i;
                }
            }
            //sort smallest according to path number
            for (int i = firstloc; i < keywordNum; i++) {
                for (int j =i + 1; j < keywordNum; j++) {
                    if (pathP.get(v).get(smallList[i]).size()>pathP.get(v).get(smallList[j]).size()) {
                        int tmp = smallList[i];
                        smallList[i] = smallList[j];
                        smallList[j] = tmp;
                    }
                }
            }

            //all ptop node is in graph
            merge(c1, ptop, v, firstloc);

            //line 13-14
            for (int i = 0; i < firstloc; i++) {
                pathP.get(v).get(smallList[i]).add(ptop);
            }

            //ling 20-25
            if (ptop.length - 1 < Util.TREEB / 2) {
                for (HopVDE it : c1.getEdges(v)){
                    int u = it.v;
                    //not visited vertex and none end-point
                    if (nodeCount[u] == 0 && c1.getEdges(u).size() > 1) {
                        Path p = new Path(ptop, u);
                        pathUpdate(c1, p);
                        if (xtop == null || p.score < xtop.getScore() * Util.B3FACC) {
                            pathU.add(p);
                        }
                        //else pruned1++;
                    }
                }
            }

            //backtrack ptop node
            tmpPath = ptop;
            while (tmpPath != null) {
                nodeCount[tmpPath.node]--;
                tmpPath = tmpPath.former;
            }
            mergeEdges.remove(mergeEdges.size() - 1);
        }

        //clear memory for next use
        pathU.clear();
        for (int i = 0; i < c1.getNodeNum(); i++) {
            pathP.get(i).clear();
        }
        pathP.clear();
    }

    private void search() throws Exception {
        xtop = null;
        setRepeatFlag();
        try {
            for (ConnectedGraph c1 : bag.cg) {
                if (!c1.keyContains(bag.keywordList)) {
                    continue;
                }
                bag.bitSet(c1);
                keywordNum = bag.getKeywordNum();
                getMSC(c1);
                bbaWT(c1);
                bbaMain(c1);
                bag.bitClear(c1);
            }
        } catch (OutOfMemoryError e) {
            pathP.clear();
            pathU.clear();
            Runtime.getRuntime().gc();
        }
        if (xtop == null) {
            throw new Exception("can't find answer.");
        }
        //xtop.printTree();
    }

    @Override
    public void search(GraphBase og1, List<String> keywordList0, Map<String, List<Integer>> mp) throws Exception{
        startTime = System.currentTimeMillis();
        if (keywordList0.size() > MAXKEYNUM) {
            System.out.println("Too many keywords to deal with,at most "+ MAXKEYNUM);
            return;
        }
        bag = (GraphB3F) og1;
        bag.givenQueryWord(keywordList0, mp);
        search();
    }

    @Override
    public void search(GraphBase og1, List<String> keywordList0) throws Exception{
        startTime = System.currentTimeMillis();
        if (keywordList0.size() > MAXKEYNUM) {
            System.out.println("Too many keywords to deal with,at most "+ MAXKEYNUM);
            return;
        }
        bag = (GraphB3F)og1;
        bag.givenQueryWord(keywordList0);
        search();
    }
}
