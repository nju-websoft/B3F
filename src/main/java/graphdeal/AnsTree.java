package graphdeal;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.*;

/**
 * @Author Yuxuan Shi
 * @Date 10/30/2019
 * @Time 4:47 PM
 */
public abstract class AnsTree implements Comparable<AnsTree>, Serializable {
    protected int root = -1;
    public Set<Integer> nodes = null;
    public List<Map.Entry<Integer, Integer>> edges = null;
    protected double score;
    @JSONField(serialize = false)
    protected transient ConnectedGraph c1 = null;

    public JsonTree JsonGenerate(String st){
        JsonTree js = new JsonTree();
        js.buildTree(this, st);
        return js;
    }

    public AnsTree(ConnectedGraph cc, int root){
        this.root = root;
        nodes = new TreeSet<>();
        nodes.add(root);
        edges = new ArrayList<>();
        score = Double.MAX_VALUE;
        c1 = cc;
    }

    /**
     * add edge u-v to tree
     * @param u end point
     * @param v end point
     */
    public void addEdge(int u, int v){
        //in a tree, it's impossible to have both nodes
        if (nodes.contains(u) && nodes.contains(v)) {
            return;
        }
        nodes.add(u);
        nodes.add(v);
        edges.add(new AbstractMap.SimpleEntry<>(u, v));
    }

    /**
     * add path from leaf to root
     * once find a node in nodes, later edges are discarded
     * @param path path from leaf to root, for path r-a-b-c from root to leaf,
     *            path should be '<b,c>,<a,b>,<r,a>'
     */
    public void addPath(List<Map.Entry<Integer, Integer>> path) {
        for (Map.Entry<Integer, Integer> it : path) {
            if (nodes.contains(it.getValue()))
                return;
            if (nodes.contains(it.getKey())) {
                nodes.add(it.getValue());
                edges.add(it);
                return;
            }
            //nodes.add(it.getKey());
            nodes.add(it.getValue());
            edges.add(it);
        }
    }

    @Override
    public int compareTo(AnsTree a2) {
        if (this.score < a2.score) {
            return 1;
        }
        return -1;
    }

    public Set<Integer> getNodes() { return nodes; }

    public List<Map.Entry<Integer, Integer>> getEdges() { return edges; }

    public double getScore(){ return score; }

    public ConnectedGraph getConnectedGraph() { return c1; }

    //public void setScore(double score){ this.score = score; }
    /**
     * calculate the score of the tree
     * @param alpha alpha in the metric
     */
    public void calcScore(double alpha) {
        double beta = 1 - alpha;
        score = 0;
        List<Integer> nodeList = new ArrayList<>(nodes);
        for (int i = 0; i < nodeList.size(); i++){
            score += alpha * c1.getNodeWeight(nodeList.get(i));
            for (int j = i + 1; j < nodeList.size(); j++) {
                score += beta * c1.sde(nodeList.get(i), nodeList.get(j));
            }
        }
        //assert checkMetric();
    }

    /**
     *
     * @return the salience score
     */
    public double getSal() {
        double sal = 0D;
        for (int node : nodes) {
            sal += c1.getNodeWeight(node);
        }
        return sal;
    }

    /**
     *
     * @return the cohesiveness score
     */
    public double getCoh() {
        double coh = 0D;
        List<Integer> nodeList = new ArrayList<>(nodes);
        for (int i = 0; i < nodeList.size(); i++){
            for (int j = i + 1; j < nodeList.size(); j++) {
                coh += c1.sde(nodeList.get(i), nodeList.get(j));
            }
        }
        return coh;
    }

    boolean checkMetric() {
        List<Integer> nodeList = new ArrayList<>(nodes);
        for (int i = 0; i < nodeList.size() - 2; i++) {

            for (int j = i + 1; j < nodeList.size() - 1; j++) {
                double ij = c1.sde(i, j);
                System.out.print(ij + " ");
                for (int k = j + 1; k < nodeList.size(); k++) {
                    double ik = c1.sde(i, k);
                    double jk = c1.sde(j, k);
                    double sum = ij + ik + jk;
                    double max = Math.max(ij, ik);
                    max = Math.max(max, jk);
                    if (sum - jk < jk) {
                        System.out.println();
                        return false;
                    }
                }
            }
        }
        System.out.println();
        return true;
    }
    /**
     * print the anstree
     */
    public void printTree(){
        System.out.println("\nRoot: " + c1.getIDToNode(root));
        //calcScore(StaticMethods.ALPHA);
        System.out.println("Score: " + score);
        System.out.println("Node number: " + nodes.size());
        for (Integer it : nodes) {
            System.out.print("node ID "+ it + " " + c1.getIDToNode(it));
            System.out.println();
        }

        /*for (Map.Entry<Integer, Integer> it : edges)
            System.out.println(it.getKey() + " " + it.getValue());*/
        for (Map.Entry<Integer, Integer> it : edges) {
            System.out.println(c1.getIDToNode(it.getKey()) + " " +
                    c1.getIDToEdge(c1.binarySearchID(it.getKey(), it.getValue()))
                    + " " + c1.getIDToNode(it.getValue()));
        }


    }

}
