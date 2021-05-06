package graphdeal;

import graphdeal.database.MysqlReader;
import graphdeal.database.MysqlWriter;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.io.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Yuxuan Shi
 * @Date 1/6/2020
 * @Time 10:03 PM
 * generate sub dbpedia related data
 */
public class WriteSubDBpedia extends GraphBase{
    private String goalGraphName;
    private int goalGraphSize;
    private boolean isSubGraph;
    private String graphFile = Util.DBPEDIA_BASE + "mappingbased_objects_en.ttl";
    private String labelFile = Util.DBPEDIA_BASE + "labels_en.ttl";
    private String typeFile = Util.DBPEDIA_BASE + "instance_types_en.ttl";
    private String tranTypeFile = Util.DBPEDIA_BASE + "instance_types_transitive_en.ttl";
    private String overWriteALL;
    private String overWriteLucene;
    private List<List<String>> rdfTriple;
    @Override
    protected void graphInit() {}

    /**
     *
     * @param goalGraphName
     * @param goalGraphSize
     */
    public void setGoalGraph(String goalGraphName, int goalGraphSize) {
        this.goalGraphName = goalGraphName;
        this.goalGraphSize = goalGraphSize;
        overWriteALL = "Danger! this function will overwrite all "
                + goalGraphName + " dataset.";
        overWriteLucene = "Danger! this function will overwrite "
                + goalGraphName + "'s lucene.";
        if (Util.DBPEDIA.equals(goalGraphName)) {
            isSubGraph = false;
        }
        else {
            isSubGraph = true;
        }
    }

    private boolean writeCheck(String warn){
        System.out.println(warn + " Continue(Y/N)?");
        Scanner inp = new Scanner(System.in);
        String cmd;
        do{
            cmd = inp.next();
            if ("Y".equals(cmd)){
                inp.close();
                return true;
            }
            if ("N".equals(cmd)){
                inp.close();
                return false;
            }
            System.out.println("Continue(Y/N)?");
        } while (true);
    }

    /**
     * generate subgraph
     */
    void generateSubGraph() {
        int totalNodeNum = graph.size();

        int sloc = 0;
        for (int r = 0; r < totalNodeNum; r++) {
            if (graph.get(sloc).size() < graph.get(r).size()) {
                sloc = r;
            }
        }

        /*
        Arrays.fill(vis, false);
        List<Integer> q = new ArrayList<>();
        for (int r = 0; r < totalNodeNum; r++) {
            cnt = r;
            if (vis[r]) {
                continue;
            }
            vis[r] = true;
            q.clear();
            q.add(r);
            int start = 0;
            while (start < q.size()) {
                int u = q.get(start);
                if (graph.get(cnt).size() < graph.get(u).size())
                    cnt = u;
                start++;
                for (HopVDE it : graph.get(u)) {
                    int v = it.v;
                    if (vis[v]) continue;
                    vis[v] = true;
                    q.add(v);
                }
            }
            if (q.size() >1000){
                break;
            }
        }
        System.out.println(cnt + " " + graph.get(cnt).size());*/

        //randomly choose nodes
        int []cDist = new int[totalNodeNum];
        Arrays.fill(cDist, Integer.MAX_VALUE);
        Random r1 = new Random();
        PriorityQueue<HopVD> ls = new PriorityQueue<>();
        ls.add(new HopVD(sloc, r1.nextDouble()));
        cDist[sloc] = 0;
        // read the graph
        List<List<HopVDE>> subgraph = new ArrayList<>();
        // hash Entity Name to Integer
        Map<String, Integer> subnodeID = new TreeMap<>();
        // rehash Integer to Entity Name
        List<String> subidToNode = new ArrayList<>();
        while (subidToNode.size() < goalGraphSize && !ls.isEmpty()) {
            int u = ls.poll().v;
            //u may enter ls multiple times
            if (!subnodeID.containsKey(idToNode.get(u))) {
                subnodeID.put(idToNode.get(u), subidToNode.size());
                subidToNode.add(idToNode.get(u));
            }
            //distance bigger than radius
            if (cDist[u] >= Util.DIAMETER / 2) {
                continue;
            }
            int cnt = 0;
            for (HopVDE it : graph.get(u)) {
                cnt++;
                //don't use too many neighbours
                if (cnt > goalGraphSize/20) {
                    break;
                }
                if (cDist[u] + 1 < cDist[it.v]){
                    ls.add(new HopVD(it.v, r1.nextDouble()));
                    cDist[it.v] = cDist[u] + 1;
                }
            }
        }

        rdfTriple = new ArrayList<>();
        //build subgraph
        ((ArrayList) subgraph).ensureCapacity(subidToNode.size());
        for (int i = 0; i < subidToNode.size(); i++) {
            subgraph.add(new ArrayList<>());
        }
        for (int i = 0; i < subidToNode.size(); i++) {
            int u = nodeID.get(subidToNode.get(i));
            for (HopVDE it : graph.get(u)) {
                if (subnodeID.containsKey(idToNode.get(it.v))) {
                    subgraph.get(i).add(new HopVDE(subnodeID.get(idToNode.get(it.v)), it.getDis(), it.edgeID));
                    if (u < it.v) {
                        rdfTriple.add(Arrays.asList(idToNode.get(u), idToEdge.get(it.edgeID), idToNode.get(it.v)));
                    }
                }
            }
        }
        int cnt = 0;
        for (List<HopVDE> list : subgraph) {
            cnt += list.size();
        }

        Arrays.fill(cDist, Integer.MAX_VALUE);

        System.out.println(subnodeID.size() + " " + subidToNode.size() + " " + cnt);
        graph = subgraph;
        nodeID = subnodeID;
        idToNode = subidToNode;


        /*//get radius
        LinkedList<Integer> q = new LinkedList<>();
        q.push(0);
        Arrays.fill(cDist, Integer.MAX_VALUE);
        cDist[0] = 0;
        int rad = 0;
        while (!q.isEmpty()) {
            int u = q.poll();
            rad = cDist[u];
            for (HopVDE it : graph.get(u)) {
                int v = it.v;
                if (cDist[v] > cDist[u] + 1) {
                    cDist[v] = cDist[u] + 1;
                    q.offerLast(v);
                }
            }
        }
        System.out.println("rad = " + rad);*/
    }

    void generateTotalGraph() {
        int totalNodeNum = graph.size();

        int sloc = 0;
        for (int r = 0; r < totalNodeNum; r++) {
            if (graph.get(sloc).size() < graph.get(r).size()) {
                sloc = r;
            }
        }

        int []cDist = new int[totalNodeNum];
        Arrays.fill(cDist, Integer.MAX_VALUE);
        Random r1 = new Random();
        PriorityQueue<HopVD> ls = new PriorityQueue<>();
        ls.add(new HopVD(sloc, 0));
        cDist[sloc] = 0;
        // read the graph
        List<List<HopVDE>> subgraph = new ArrayList<>();
        // hash Entity Name to Integer
        Map<String, Integer> subnodeID = new TreeMap<>();
        // rehash Integer to Entity Name
        List<String> subidToNode = new ArrayList<>();
        while (!ls.isEmpty()) {
            int u = ls.poll().v;
            if (!subnodeID.containsKey(idToNode.get(u))) {
                subnodeID.put(idToNode.get(u), subidToNode.size());
                subidToNode.add(idToNode.get(u));
            }
            for (HopVDE it : graph.get(u)) {
                if (cDist[u] + 1 < cDist[it.v]){
                    cDist[it.v] = cDist[u] + 1;
                    ls.add(new HopVD(it.v, cDist[it.v]));
                }
            }
        }

        rdfTriple = new ArrayList<>();
        //build subgraph
        ((ArrayList) subgraph).ensureCapacity(subidToNode.size());
        for (int i = 0; i < subidToNode.size(); i++) {
            subgraph.add(new ArrayList<>());
        }
        for (int i = 0; i < subidToNode.size(); i++) {
            int u = nodeID.get(subidToNode.get(i));
            for (HopVDE it : graph.get(u)) {
                if (subnodeID.containsKey(idToNode.get(it.v))) {
                    subgraph.get(i).add(new HopVDE(subnodeID.get(idToNode.get(it.v)), it.getDis(), it.edgeID));
                    if (u < it.v) {
                        rdfTriple.add(Arrays.asList(idToNode.get(u), idToEdge.get(it.edgeID), idToNode.get(it.v)));
                    }
                }
            }
        }
        int cnt = 0;
        for (List<HopVDE> list : subgraph) {
            cnt += list.size();
        }

        Arrays.fill(cDist, Integer.MAX_VALUE);

        System.out.println(subnodeID.size() + " " + subidToNode.size() + " " + cnt);
        graph = subgraph;
        nodeID = subnodeID;
        idToNode = subidToNode;
    }

    /**
     * write all data to database goalGraphName
     */
    public void writeAll(String subGraphName, int size) {
        setGoalGraph(subGraphName, size);
        if (!writeCheck(overWriteALL)) {
            return;
        }
        System.out.println("OK, writing database!");
        MysqlWriter mw = null;
        try {
            mw = new MysqlWriter(goalGraphName);
            graphName = goalGraphName;

            //read graph
            Model model = ModelFactory.createDefaultModel();
            model.read(graphFile);
            StmtIterator iter = model.listStatements();
            String x, y, z;
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                x = stmt.getSubject().toString();
                y = stmt.getPredicate().toString();
                z = stmt.getObject().toString();
                int u = tryAddNode(x);
                int v = tryAddNode(z);
                int uv = tryAddEdge(y);
                graph.get(u).add(new HopVDE(v, 1D, uv));
                graph.get(v).add(new HopVDE(u, 1D, uv));
            }
            model.close();

            if (isSubGraph) {
                generateSubGraph();
            }
            else {
                generateTotalGraph();
            }

            PrintWriter pw;
            //write rdf triple
            pw = new PrintWriter(Util.getRdfTriple(goalGraphName));
            for (List<String> triple : rdfTriple) {
                pw.println(triple.get(0) + " " + triple.get(1) + " " + triple.get(2));
            }
            pw.close();
            pw = new PrintWriter(Util.getEntity(goalGraphName));
            for (String st : nodeID.keySet()) {
                pw.println(st);
            }
            pw.close();

            //write subgraph triple
            mw.dbInit("graphTriple");
            model = ModelFactory.createDefaultModel();
            model.read(graphFile);
            iter = model.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                x = stmt.getSubject().toString();
                y = stmt.getPredicate().toString();
                z = stmt.getObject().toString();
                if (!nodeID.containsKey(x)) {
                    continue;
                }
                if (!nodeID.containsKey(z)) {
                    continue;
                }
                mw.insertGraphTriple(x, y, z);
            }
            model.close();
            mw.writeEnd();

            //read label
            model = ModelFactory.createDefaultModel();
            model.read(labelFile);
            iter = model.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                x = stmt.getSubject().toString();
                if (!nodeID.containsKey(x)) {
                    continue;
                }
                int u = nodeID.get(x);
                z = stmt.getObject().toString();
                z = z.substring(0, z.length() - 3);
                labelList.get(u).add(z);
            }
            model.close();

            //read type && write type triple to database
            mw.dbInit("typeTriple");
            Vector<InputStream> ins = new Vector<>();
            ins.add(FileManager.get().open(typeFile));
            ins.add(FileManager.get().open(tranTypeFile));
            InputStream ss = new SequenceInputStream(ins.elements());
            model = ModelFactory.createDefaultModel();
            model.read(ss, null, "TTL");
            iter = model.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                x = stmt.getSubject().toString();
                if (!nodeID.containsKey(x)) {
                    continue;
                }
                int u = nodeID.get(x);
                y = stmt.getPredicate().toString();
                z = stmt.getObject().toString();
                mw.insertTypeTriple(x, y, z);
                short typeNum = tryAddType(z);
                typeList.get(u).add(typeNum);
            }
            model.close();
            mw.writeEnd();

            if (Util.LABEL_SHORT) {
                for (int i = 0; i < typeList.size(); i++) {
                    typeList.set(i, typeList.get(i).stream().
                            distinct().sorted().
                            collect(Collectors.toList()));
                }
            }

            //get page rank
            Graph<Integer, DefaultEdge> jGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            for (int i = 0; i < graph.size(); i++) {
                jGraph.addVertex(i);
            }
            for (int u = 0; u < graph.size(); u++) {
                for (HopVDE it : graph.get(u)) {
                    jGraph.addEdge(u, it.v);
                }
            }
            PageRank<Integer, DefaultEdge> pageRank = new PageRank<>(jGraph);

            double maxPR = 0;
            double minPR = Double.MAX_VALUE;
            for (int i = 0; i < graph.size(); i++) {
                maxPR = Math.max(maxPR, pageRank.getVertexScore(i));
                minPR = Math.min(minPR, pageRank.getVertexScore(i));
            }
            //wirte nodeID
            mw.dbInit("nodeID");
            for (Map.Entry<String, Integer> it : nodeID.entrySet()) {
                double  pr = 0D;
                pr = Math.log10(pageRank.getVertexScore(it.getValue())) - Math.log10(minPR);
                pr = 1- 1/(1+ Math.exp((-pr)));
                mw.insertNodeID(it.getKey(), it.getValue(), pr);
            }
            mw.writeEnd();

            //write typeID
            mw.dbInit("typeID");
            for (Map.Entry<String, Short> it : typeID.entrySet()) {
                mw.insertTypeID(it.getKey(), it.getValue());
            }
            mw.writeEnd();

            //write type
            mw.dbInit("type");
            for (int i = 0; i < typeList.size(); i++) {
                for (short it : typeList.get(i)) {
                    mw.insertType(i, it);
                }
            }
            mw.writeEnd();
            mw.close();
            componentGen();
        } catch (SQLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Build lucene with node's map stored in database
     */
    @Deprecated
    public void buildLuceneWithDB() {
        if (!writeCheck(overWriteLucene)) {
            return;
        }
        System.out.println("OK, writing lucene!");
        try (MysqlReader mr = new MysqlReader(goalGraphName)) {
            graphName = goalGraphName;
            {
                mr.dbInit("nodeID");
                String[] a = new String[1];
                int[] b = new int[1];
                while (mr.readNodeID(a, b))
                    nodeID.put(a[0], b[0]);
                ((ArrayList) idToNode).ensureCapacity(nodeID.size());
                for (int i = 0; i < nodeID.size(); i++) idToNode.add(null);
                for (Map.Entry<String, Integer> entry : nodeID.entrySet())
                    idToNode.set(entry.getValue(), entry.getKey());
                ((ArrayList) graph).ensureCapacity(nodeID.size());
                for (int i = 0; i < nodeID.size(); i++) graph.add(new ArrayList<>());

                ((ArrayList) labelList).ensureCapacity(nodeID.size());
                for (int i = 0; i < nodeID.size(); i++) labelList.add(new ArrayList<>());
            }

            //read graph
            Model model = ModelFactory.createDefaultModel();
            model.read(graphFile);
            StmtIterator iter = model.listStatements();
            String x, y, z;
            while (iter.hasNext()) {
                // get next statement
                Statement stmt = iter.nextStatement();
                // get the triple
                x = stmt.getSubject().toString();
                y = stmt.getPredicate().toString();
                z = stmt.getObject().toString();
                if (!nodeID.containsKey(x)) {
                    continue;
                }
                if (!nodeID.containsKey(z)) {
                    continue;
                }
                int u = nodeID.get(x);
                int v = nodeID.get(z);
                int uv = tryAddEdge(y);
                graph.get(u).add(new HopVDE(v, 1D, uv));
                graph.get(v).add(new HopVDE(u, 1D, uv));
            }
            model.close();

            //read label
            model = ModelFactory.createDefaultModel();
            model.read(labelFile);
            iter = model.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement(); // get next statement
                x = stmt.getSubject().toString(); // get the subject
                if (!nodeID.containsKey(x)) continue;
                int u = nodeID.get(x);
                z = stmt.getObject().toString();
                z = z.substring(0, z.length() - 3); //withdraw "@en"
                labelList.get(u).add(z);
            }
            model.close();
            componentGen();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    void componentGen(){
        int totalNodeNum = graph.size();
        boolean[] vis = new boolean[totalNodeNum];
        Arrays.fill(vis, false);
        List<Integer> q = new ArrayList<Integer>();
        int cnt = 0;
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
            if (q.size() >= Util.MINCOMPONENT) {
                new SubDBpediaConnectedGraph(this, q, graphName, cnt);
                System.out.println(q.size());
                cnt++;
            }
        }
    }

}

class SubDBpediaConnectedGraph {
    private WriteSubDBpedia gb;
    // rehash Integer to Entity Name
    private List<String> idToNode;
    private String graphName = null;
    private int id;

    SubDBpediaConnectedGraph(WriteSubDBpedia gb1, List<Integer> connected, String graphName1, int id1) {
        gb = gb1;
        graphName = graphName1;
        id = id1;
        Map<Integer, Integer> nodeMap = new TreeMap<>();
        idToNode = new ArrayList<>(connected.size());
        for (int it : connected) {
            nodeMap.put(it, idToNode.size());
            idToNode.add(gb.idToNode.get(it));
        }
        buildLucene(nodeMap, connected);
    }

    /**
     * build Lucene index
     * @param nodeMap map nodes from origin graph to the component
     * @param connected connected nodes
     */
    void buildLucene(Map<Integer, Integer> nodeMap, List<Integer> connected){
        try {
            String cgDir = Util.getLucenePath(graphName, id);
            //check if index is built
            Directory dir = FSDirectory.open(Paths.get(cgDir));
            if (DirectoryReader.indexExists(dir)){  //if the index has been built, delete it
                FileUtils.deleteQuietly(new File(cgDir));
                dir.close();
                dir = FSDirectory.open(Paths.get(cgDir));
            }
            //PrintWriter pw = new PrintWriter(StaticMethods.getLabelWriterPath(graphName, id));
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setRAMBufferSizeMB(1024.0);
            IndexWriter indexWriter = new IndexWriter(dir, iwc);
            for (int it : connected) {
                int u = nodeMap.get(it);
                StringBuilder sb = new StringBuilder();
                for (String nodeLabel: gb.labelList.get(it)){
                    sb.append(" ");
                    sb.append(nodeLabel);
                }
                if (sb.length() == 0) continue;
                String nodeLabels = sb.substring(1, sb.length());

                Document document = new Document();
                document.add(new StringField("id", Integer.toString(u), Field.Store.YES));
                document.add(new TextField("label", nodeLabels, Field.Store.YES));
                indexWriter.addDocument(document);

            }
            indexWriter.forceMerge(1);
            indexWriter.close();
            dir.close();
            System.out.println(cgDir + " is built!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}