package experiment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dpbf.GraphDPBF;
import graphdeal.ConnectedGraph;
import graphdeal.GraphBase;
import graphdeal.JsonTree;
import graphdeal.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 *
 * the class to find the
 * @Author Yuxuan Shi
 * @Date 2020/10/12
 * @Time 22:55
 **/
public class FindCase {
    /***
     * check whether two trees are similar
     * @param jsobA json tree a
     * @param jsobB json tree b
     * @return true if similar
     */
    boolean notSimilar(JSONObject jsobA, JSONObject jsobB) {
        List<Integer> nodesA = ((JSONArray) jsobA.get("nodes")).toJavaList(Integer.class);
        List<Integer> nodesB = ((JSONArray) jsobB.get("nodes")).toJavaList(Integer.class);
        Map<Integer, Integer> nodeCnt = new TreeMap<>();

        for (Integer node : nodesA) {
            nodeCnt.putIfAbsent(node, 1);
        }

        int cnt = 0;
        for (Integer node : nodesB) {
            if (nodeCnt.containsKey(node)) {
                cnt++;
            }
        }
        return (cnt < nodesA.size() - 1&& cnt < nodesB.size() - 1);
    }

    /**
     * test whether the tree contains united state
     * @param jsob the json object of the tree
     * @param gb the graph
     * @return false if doesn't contain united state
     */
    boolean notUnited(JSONObject jsob, GraphBase gb) {
        String united = "http://dbpedia.org/resource/United_States";
        List<Integer> nodes = ((JSONArray) jsob.get("nodes")).toJavaList(Integer.class);
        for (ConnectedGraph c1 : gb.cg) {
            for (Integer it : nodes) {
                if (united.equals(c1.getIDToNode(it))) {
                    return false;
                }
            }
        }
        return true;
    }

    void printTree(PrintWriter fop, GraphBase gb, String name, JSONObject jsob) {
        printTree(fop, gb, name, jsob, true);
    }

    void printTree(PrintWriter fop, GraphBase gb, String name, JSONObject jsob, boolean row) {
        fop.println(name);
        for (ConnectedGraph c1 : gb.cg) {
            List<Integer> nodes = ((JSONArray) jsob.get("nodes")).toJavaList(Integer.class);
            if (row) {
                fop.println("nodes " + nodes.size());
                for (Integer it : nodes) {
                    fop.print(c1.getIDToNode(it) + "\n");
                }
            }


            JSONArray excelArray = (JSONArray) jsob.get("edges");
            if (row) {
                fop.println("edges " + excelArray.size());
            }
            for (int id = 0; id < excelArray.size(); id++) {
                JSONObject jb = (JSONObject) excelArray.get(id);
                for (Map.Entry jben : jb.entrySet()) {
                    int key = (Integer) jben.getKey();
                    int val = (Integer) jben.getValue();
                    if (row) {
                        fop.println(c1.getIDToNode(key) + " " +
                                c1.getIDToEdge(c1.binarySearchID(key, val))
                                + " " + c1.getIDToNode(val));
                    }
                    else {
                        fop.println(JsonTree.formatEntity(c1.getIDToNode(key)) + " -- " +
                                JsonTree.formatlink(c1.getIDToEdge(c1.binarySearchID(key, val)))
                                + " -- " + JsonTree.formatEntity(c1.getIDToNode(val)));
                    }
                }
            }
            fop.println();
        }
    }
    void findDifferent() throws FileNotFoundException {
        double []als = new double[]{0.3, 0.7};

        String graph = Util.getInitPPS().get("GRAPH_NAME").toString();
        Util.setUSERDF2VEC(false);

        int queryNum = 0;
        //read dpbf json
        Util.setAlpha(0.5);
        Scanner sc = new Scanner(new File(Util.getAnsJs(graph, "DPBFBase")));
        List<JSONObject> jsdp = new ArrayList<>();
        while (sc.hasNext()) {
            String obj = sc.nextLine();
            jsdp.add(JSONObject.parseObject(obj));
        }
        sc.close();

        //read b3f json
        Map<Double, List<JSONObject>> jsb3fs = new TreeMap<>();
        for (double al : als) {
            Util.setAlpha(al);
            sc = new Scanner(new File(Util.getAnsJs(graph, "b3f")));
            List<JSONObject> jseo = new ArrayList<>();
            while (sc.hasNext()) {
                String obj = sc.nextLine();
                jseo.add(JSONObject.parseObject(obj));
            }
            jsb3fs.put(al, jseo);
            sc.close();
        }

        //read b3f weight
        Map<Double, List<AnsSummary.QueryInfo>> rb3f = new TreeMap<>();
        for (double al : als) {
            Util.setAlpha(al);
            rb3f.put(al, AnsSummary.readAnsTxt(graph, "b3f"));
        }
        //read dp weight
        Map<Double, List<AnsSummary.QueryInfo>> rdp = new TreeMap<>();
        for (double al : als) {
            Util.setAlpha(al);
            rdp.put(al, AnsSummary.readAnsTxt(graph, "DPBFBase"));
        }
        GraphDPBF gb = GraphDPBF.getInstance();
        PrintWriter fop = new PrintWriter(new File(Util.realFilePath("E:\\data\\result.txt")));

        for (int i = 0; i < jsdp.size(); i++) {

            boolean diff = false;
            for (double al : als) {
                if (Math.abs(rdp.get(al).get(i).getCoh() - rb3f.get(al).get(i).getCoh()) > 1e-3) {
                    diff = true;
                    break;
                }
            }

            if (!diff) {
                continue;
            }
            // make sure b3f and dpbf is different
            if (!notSimilar(jsb3fs.get(0.3).get(i), jsdp.get(i))) {
                continue;
            }

            if (!notUnited(jsb3fs.get(0.3).get(i), gb)) {
                continue;
            }
            if (!notUnited(jsdp.get(i), gb)) {
                continue;
            }


            queryNum++;
            //print query
            List<String> word = rdp.get(0.3).get(i).getQuery();
            for (int num = 0; num < word.size() -  1; num++) {
                fop.print(word.get(num) + ';');
            }
            fop.println(word.get(word.size() - 1));

            //all trees to be printed
            for (int id = 0; id < als.length; id++) {
                double al = als[id];
                fop.println(al + ".B3F");
                boolean appear = false;
                for (int prev = 0; prev < id; prev++) {
                    if (Math.abs(rb3f.get(al).get(i).getCoh() - rb3f.get(als[prev]).get(i).getCoh()) < 1e-3) {
                        appear = true;
                        fop.println(rb3f.get(als[prev]).get(i).getSal() + " " + rb3f.get(als[prev]).get(i).getCoh());
                        fop.println(als[prev] + ".B3F");
                        break;
                    }
                }
                if (!appear) {
                    fop.println(rb3f.get(al).get(i).getSal() + " " + rb3f.get(al).get(i).getCoh());
                    printTree(fop, gb, al + ".B3F", jsb3fs.get(al).get(i));
                }
            }

            fop.println("DPBF");
            boolean appear = false;
            for (double al : als) {
                if (Math.abs(rdp.get(al).get(i).getCoh() - rb3f.get(al).get(i).getCoh()) < 1e-3) {
                    appear = true;
                    fop.println(rb3f.get(al).get(i).getSal() + " " + rb3f.get(al).get(i).getCoh());
                    fop.println(al + ".B3F");
                    break;
                }
            }
            if (!appear) {
                fop.println(rdp.get(0.3).get(i).getSal() + " " + rdp.get(0.3).get(i).getCoh());
                printTree(fop, gb, "DPBF", jsdp.get(i));
            }
        }
        fop.close();
        System.out.println(queryNum);
        GraphDPBF.closeInstance();
    }

    public static void main(String[] args) {
        try {
            FindCase f1 = new FindCase();
            //f1.testCases();
            //f1.findDifferent();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
