package graphdeal;

import graphdeal.database.MysqlReader;
import org.apache.jena.rdf.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Yuxuan Shi
 * @Date 10/30/2019
 * @Time 10:29 AM
 */
public abstract class GraphBase{
    protected int biggestNodeNum;
    /**
     * graph
     */
    List<List<HopVDE>> graph;

    /**
     * hash Entity Name to Integer
     */
    Map<String, Integer> nodeID;

    /**
     * rehash Integer to Entity Name
     */
    List<String> idToNode;
    // hash edge name to Integer
    Map<String, Integer> edgeID;
    // rehash Integer to Entity Name
    List<String> idToEdge;
    // type hash
    Map<String, Short> typeID;
    //type of any node
    List<List<Short>> typeList;
    List<List<String>> labelList;
    // node weight
    List<Double> nodeWeight;
    /**
     * rdf vecotr
     */
    List<List<Double>> rdf2vec;

    //list all connected component
    public List<ConnectedGraph> cg;
    public List<String> keywordList;
    protected int queryKeyNum;
    protected String graphName;

    protected abstract void graphInit();

    /**
     *
     */
    public GraphBase(){
        graph = new ArrayList<>();
        nodeID = new TreeMap<>();
        idToNode = new ArrayList<>();
        edgeID = new TreeMap<>();
        idToEdge = new ArrayList<>();
        typeID = new TreeMap<>();
        nodeWeight = new ArrayList<>();
        typeList = new ArrayList<>();
        labelList = new ArrayList<>();
        cg = new ArrayList<>();
        rdf2vec = new ArrayList<>();
    }

   /* public GraphBase(DogReader d1, String graphName){
        graph = new ArrayList<>();
        nodeID = new TreeMap<>();
        idToNode = new ArrayList<>();
        edgeID = new TreeMap<>();
        idToEdge = new ArrayList<>();
        typeID = new TreeMap<>();
        nodeWeight = new ArrayList<>();
        typeList = new ArrayList<>();
        labelList = new ArrayList<>();
        cg = new ArrayList<>();
        this.graphName = graphName;
        readGraph(d1, graphName);
    }*/

    int tryAddNode(String node){
        if (!nodeID.containsKey(node)){
            nodeID.put(node, nodeID.size());
            graph.add(new ArrayList<>());
            idToNode.add(node);
            //prepare type list for new node
            typeList.add(new ArrayList<>());
            labelList.add(new ArrayList<>());
        }
        return nodeID.get(node);
    }

    int tryAddEdge(String edge){
        if (!edgeID.containsKey(edge)){
            edgeID.put(edge, edgeID.size());
            idToEdge.add(edge);
        }
        return edgeID.get(edge);
    }

    short tryAddType(String type){
        if (!typeID.containsKey(type)) {
            typeID.put(type, (short)typeID.size());
        }
        return typeID.get(type);
    }

    /**
     * read single ttl file
     * @param file the file to be read
     */
    protected void readGraphProcess(File file){
        try(InputStream in = new FileInputStream(file)){
            Model model = ModelFactory.createDefaultModel();
            model.read(in, null, "TTL");
            StmtIterator iter = model.listStatements();
            String x, y, z;
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement(); // get next statement
                RDFNode object = stmt.getObject();
                Property predicate = stmt.getPredicate();
                // get the subject
                x = stmt.getSubject().toString();
                // get the predicate
                y = predicate.toString();
                z = object.toString();
                int u = tryAddNode(x);
                if (object.isLiteral()){
                    //label node
                    if ("label".equals(predicate.getLocalName())){
                        if (!z.endsWith("@en")) {
                            continue;
                        }
                        labelList.get(u).add(z.substring(0, z.length() - 3));
                    }
                }
                if (object.isResource()){
                    if (z.equals("http://www.w3.org/2002/07/owl#NamedIndividual")||
                    z.equals("http://www.w3.org/2002/07/owl#Class")) {
                        /*int typeNum = tryAddType(z);
                        typeList.get(u).add(typeNum);*/
                        continue;
                    }
                    int v = tryAddNode(z);
                    int uv = tryAddEdge(y);
                    graph.get(u).add(new HopVDE(v, uv));
                    graph.get(v).add(new HopVDE(u, uv));

                    if ("type".equals(predicate.getLocalName())){
                        short typeNum = tryAddType(z);
                        typeList.get(u).add(typeNum);
                        typeList.get(v).add(typeNum);   //type node's type is itself !!!
                    }
                }
            }
            model.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //read a single type file
    protected void readTypeFile(File file){
        try (InputStream in = new FileInputStream(file)){
            Model model = ModelFactory.createDefaultModel();
            model.read(in, null, "TTL");
            StmtIterator iter = model.listStatements();
            String x, y, z;
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement(); // get next statement
                RDFNode object = stmt.getObject();
                x = stmt.getSubject().toString(); // get the subject
                y = stmt.getPredicate().toString();
                z = object.toString();
                if (!nodeID.containsKey(x)) {
                    continue;
                }
                int u = nodeID.get(x);
                int v = tryAddNode(z);
                int uv = tryAddEdge(y);
                graph.get(u).add(new HopVDE(v, uv));
                graph.get(v).add(new HopVDE(u, uv));
                short typeNum = tryAddType(z);
                typeList.get(u).add(typeNum);
                typeList.get(v).add(typeNum);   //type node's type is itself !!!
            }
            model.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //read all type files
    protected void readTypeFiles(String typeFileOrDir){
        File tryFile = new File(typeFileOrDir);
        if (!tryFile.isDirectory()){    //a file
            readTypeFile(tryFile);
        }
        else {
            File[] files = tryFile.listFiles(); //a directory
            if (files.length == 0) {
                System.out.println("it is empty!");
                return;
            }
            for (File file : files) {
                readTypeFile(file);
            }
        }
    }

    //init typelist and generate all connected graph
    private void graphPrepare(){
        if (Util.LABEL_SHORT) {
            for (int i = 0; i < typeList.size(); i++) {
                typeList.set(i, typeList.get(i).stream().distinct().sorted().collect(Collectors.toList()));
            }
        }

        componentGen();
        graphInit();
    }

    //judge whether file is a directory

    /**
     * @param fileOrDir
     */
    private void readGraph(String fileOrDir){
        File tryFile = new File(fileOrDir);
        if (!tryFile.isDirectory()){    //a file
            readGraphProcess(tryFile);
        }
        else {
            File[] files = tryFile.listFiles(); //a directory
            if (files.length == 0) {
                System.out.println("it is empty!");
                return;
            }
            for (File file : files) {
                readGraphProcess(file);
            }
        }
    }

    /**
     * judge whether file is a directory
     * @param fileOrDir
     * @param typeFileOrDir
     * @param graphName
     */
    protected void readGraph(String fileOrDir,String typeFileOrDir, String graphName){
        this.graphName = graphName;
        readGraph(fileOrDir);
        if (typeFileOrDir != null) {
            readTypeFiles(typeFileOrDir);
        }
        //init setting
        for (int i = 0; i < graph.size(); i++) {
            nodeWeight.add(1D);
        }
        graphPrepare();
    }

    /**
     * @param node vertex
     * @param weight u's weight
     */
    void tryAddWeight(int node, double weight) {
        if (node >= nodeWeight.size()) {
            for (int i = nodeWeight.size(); i <= node; i++) {
                nodeWeight.add(0D);
            }
        }
        nodeWeight.set(node, weight);
    }

    /**
     * read data from mysql graphdeal.database
     * @param database graphdeal.database of the dataset
     * @param graphName the graph name in the graphdeal.database
     */
    protected void readGraph(String database, String graphName){
        try (MysqlReader mr = new MysqlReader(database)){
            this.graphName = graphName;
            List<Object> mrRes = null;

            mr.dbInit("nodeID");
            while ((mrRes = mr.readNodeID()) != null) {
                nodeID.put((String) mrRes.get(0), (Integer) mrRes.get(1));
                tryAddWeight((Integer) mrRes.get(1), (Double) mrRes.get(2));
            }

            ((ArrayList) idToNode).ensureCapacity(nodeID.size());
            for (int i = 0; i < nodeID.size(); i++) {
                idToNode.add(null);
            }
            for (Map.Entry<String, Integer> entry : nodeID.entrySet()) {
                idToNode.set(entry.getValue(), entry.getKey());
            }
            ((ArrayList) graph).ensureCapacity(nodeID.size());
            for (int i = 0; i < nodeID.size(); i++) {
                graph.add(new ArrayList<>());
            }

            ((ArrayList) typeList).ensureCapacity(nodeID.size());
            for (int i = 0; i < nodeID.size(); i++) {
                typeList.add(new ArrayList<>());
            }

            //read graph triple
            mr.dbInit("graphTriple");
            while((mrRes = mr.readGraphTriple()) != null){
                int u = nodeID.get((String) mrRes.get(0));
                int v = nodeID.get((String) mrRes.get(2));
                int uv = tryAddEdge((String) mrRes.get(1));
                graph.get(u).add(new HopVDE(v, uv));
                graph.get(v).add(new HopVDE(u, uv));
            }

            //read type's id
            mr.dbInit("typeID");
            while ((mrRes = mr.readTypeID()) != null) {
                typeID.put((String)mrRes.get(0), (Short)mrRes.get(1));
            }

            //read type and its
            mr.dbInit("type");
            while ((mrRes = mr.readType()) != null) {
                typeList.get((Integer) mrRes.get(0)).add((Short) mrRes.get(1));
            }

            if (Util.USERDF2VEC) {
                mr.dbInit("rdf2vec");
                while ((mrRes = mr.readRdf2Vec()) != null) {
                    List<Double> vector = new ArrayList<>();
                    for (int i = 1; i < mrRes.size(); i++) {
                        vector.add((Double) mrRes.get(i));
                    }
                    rdf2vec.add(vector);
                }
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        //we don't read
        graphPrepare();
    }

    void componentGen(){
        int totalNodeNum = 0, edgeNum = 0;
        totalNodeNum = graph.size();
        for (List<HopVDE> it : graph) {
            edgeNum += it.size();
        }
        System.out.println("nodeNum " + totalNodeNum);
        System.out.println("edgeNum " + edgeNum /2);

        biggestNodeNum = 0;
        boolean[] vis = new boolean[totalNodeNum];
        Arrays.fill(vis, false);
        List<Integer> q = new ArrayList<Integer>();
        for(int r = 0; r < totalNodeNum; r++) {
            if (vis[r]) {
                continue;
            }
            vis[r]= true;
            q.clear();
            q.add(r);
            int start = 0;
            while (start < q.size()) {
                int u = q.get(start);
                start++;
                for (HopVDE it : graph.get(u)) {
                    int v = it.v;
                    if (vis[v]) {
                        continue;
                    }
                    vis[v] = true;
                    q.add(v);
                }
            }
            if (q.size() > Util.MINCOMPONENT) {
                cg.add(new ConnectedGraph(this, q, graphName, cg.size()));
                if (q.size() > biggestNodeNum) {
                    biggestNodeNum = q.size(); //biggestNodeNum
                }
            }
        }

        for (List<HopVDE> it : graph) {
            it.clear();
        }
        graph.clear();
        nodeID.clear();
        idToNode.clear();
        edgeID.clear();
        typeID.clear();
        nodeWeight.clear();
        typeList.clear();
        rdf2vec.clear();
    }

    public void givenQueryWord(List<String> a){
        try {
            keywordList = a;
            queryKeyNum = keywordList.size();
            for (ConnectedGraph c1 : cg){
                c1.givenQueryWord(a);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void givenQueryWord(List<String> a, Map<String, List<Integer>> amp) {
        keywordList = a;
        queryKeyNum = keywordList.size();
        for (ConnectedGraph c1 : cg){
            c1.givenQueryWord(a, amp);
        }
    }

    /**
     * test whether lucene index contains this keyword
     * @param st keyword to be tested
     * @return true if containing
     */
    public boolean containQueryWord(String st){
        for (ConnectedGraph c1 : cg) {
            if (c1.containQueryWord(st)) {
                return true;
            }
        }
        return false;
    }

    public boolean containQueryWords(List<String> st){
        for (String word : st) {
            if (!containQueryWord(word)) {
                return false;
            }
        }
        return true;
    }

    public int getKeywordNum(){
        return queryKeyNum;
    }

    public void close(){
        for (ConnectedGraph c1 : cg){
            c1.close();
        }
        idToEdge.clear();
    }
}
