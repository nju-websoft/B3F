package graphdeal;

import graphdeal.database.MysqlReader;
import graphdeal.database.MysqlWriter;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.*;
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

import java.io.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Yuxuan Shi
 * @Date 1/6/2020
 * @Time 10:03 PM
 * generate dbpedia related data
 */
public class WriteDBpedia extends GraphBase {

    private final String base = "F:\\data\\" + Util.DBPEDIA + "\\";
    private final String labelFile = base + "labels_en.ttl";
    private final String graphFile = base + "mappingbased_objects_en.ttl";
    private final String typeFile = base + "instance_types_en.ttl";
    private final String tranTypeFile = base + "instance_types_transitive_en.ttl";
    private final String overWriteALL = "Danger! this function will overwrite all "
            + Util.DBPEDIA + " dataset.";
    private final String overWriteLucene = "Danger! this function will overwrite "
            + Util.DBPEDIA + "'s lucene.";

    @Override
    protected void graphInit() {
    }

    private boolean writeCheck(String warn) {
        System.out.println(warn + " Continue(Y/N)?");
        Scanner inp = new Scanner(System.in);
        String cmd;
        do {
            cmd = inp.next();
            if ("Y".equals(cmd)) {
                inp.close();
                return true;
            }
            if ("N".equals(cmd)) {
                inp.close();
                return false;
            }
            System.out.println("Continue(Y/N)?");
        } while (true);
    }

    /**
     * write all data to database 'Util.DBPEDIA'
     */
    public void writeAll() {
        if (!writeCheck(overWriteALL)) {
            return;
        }
        System.out.println("OK, writing database!");
        MysqlWriter mw = null;
        try {
            mw = new MysqlWriter(Util.DBPEDIA);

            graphName = Util.DBPEDIA;
            //write graph triple
            //mw.dbInit("graphTriple");
            Model model = ModelFactory.createDefaultModel();
            model.read(graphFile);
            StmtIterator iter = model.listStatements();
            String x, y, z;
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                RDFNode object = stmt.getObject();
                Property predicate = stmt.getPredicate();
                x = stmt.getSubject().toString();
                y = predicate.toString();
                z = object.toString();
                int u = tryAddNode(x);
                int v = tryAddNode(z);
                int uv = tryAddEdge(y);
                //mw.insertGraphTriple(x, y, z);
                graph.get(u).add(new HopVDE(v, 1D, uv));
                graph.get(v).add(new HopVDE(u, 1D, uv));
            }
            model.close();
            //mw.writeEnd();

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
            //mw.dbInit("typeTriple");
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
                //mw.insertTypeTriple(x, y, z);
                short typeNum = tryAddType(z);
                typeList.get(u).add(typeNum);
            }
            model.close();
            //mw.writeEnd();

            if (Util.LABEL_SHORT) {
                for (int i = 0; i < typeList.size(); i++) {
                    typeList.set(i, typeList.get(i).stream().
                            distinct().sorted().
                            collect(Collectors.toList()));
                }
            }

            //wirte nodeID
            /*mw.dbInit("nodeID");
            for (Map.Entry<String, Integer> it : nodeID.entrySet())
                mw.insertnodeID(it.getKey(), it.getValue());
            mw.writeEnd();

            //write typeID
            mw.dbInit("typeID");
            for (Map.Entry<String, Short> it : typeID.entrySet())
                mw.insertTypeID(it.getKey(), it.getValue());
            mw.writeEnd();

            //write type
            mw.dbInit("type");
            for (int i = 0; i < typeList.size(); i++) {
                for (short it : typeList.get(i))
                    mw.insertType(i, it);
            }
            mw.writeEnd();
            mw.close();*/
            componentGen();
        } catch (SQLException e) {
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
        try (MysqlReader mr = new MysqlReader(Util.DBPEDIA)) {
            graphName = Util.DBPEDIA;
            {
                mr.dbInit("nodeID");
                String[] a = new String[1];
                int[] b = new int[1];
                while (mr.readNodeID(a, b)) {
                    nodeID.put(a[0], b[0]);
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

                ((ArrayList) labelList).ensureCapacity(nodeID.size());
                for (int i = 0; i < nodeID.size(); i++) {
                    labelList.add(new ArrayList<>());
                }
            }

            //read graph
            Model model = ModelFactory.createDefaultModel();
            model.read(graphFile);
            StmtIterator iter = model.listStatements();
            String x, y, z;
            while (iter.hasNext()) {
                // get next statement
                Statement stmt = iter.nextStatement();
                // get the subject, predicate and object
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
                Statement stmt = iter.nextStatement();
                x = stmt.getSubject().toString();
                if (!nodeID.containsKey(x)) {
                    continue;
                }
                int u = nodeID.get(x);
                z = stmt.getObject().toString();
                //withdraw "@en"
                z = z.substring(0, z.length() - 3);
                labelList.get(u).add(z);
            }
            model.close();
            componentGen();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    void componentGen() {
        int totalNodeNum = graph.size();
        boolean[] vis = new boolean[totalNodeNum];
        Arrays.fill(vis, false);
        List<Integer> q = new ArrayList<Integer>();
        int cnt = 0;
        for (int r = 0; r < totalNodeNum; r++) {
            if (vis[r]) {
                continue;
            }
            vis[r] = true;
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
                new DBpediaConnectedGraph(this, q, graphName, cnt);
                System.out.println(q.size());
                cnt++;
            }
        }
    }

    /**
     * get the connected componment of DBpedia
     */
    public void testConnection() {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(base + "connection.txt");
            graphName = Util.DBPEDIA;
            Model model = ModelFactory.createDefaultModel();
            model.read(graphFile);
            StmtIterator iter = model.listStatements();
            String x, y, z;
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement(); // get next statement
                RDFNode object = stmt.getObject();
                Property predicate = stmt.getPredicate();
                x = stmt.getSubject().toString(); // get the subject
                y = predicate.toString(); // get the predicate
                z = object.toString();
                int u = tryAddNode(x);
                int v = tryAddNode(z);
                int uv = tryAddEdge(y);
                graph.get(u).add(new HopVDE(v, 1D, uv));
                graph.get(v).add(new HopVDE(u, 1D, uv));
            }
            model.close();

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

            int totalNodeNum = graph.size();
            boolean[] vis = new boolean[totalNodeNum];
            Arrays.fill(vis, false);
            List<Integer> q = new ArrayList<Integer>();
            for (int r = 0; r < totalNodeNum; r++) {
                if (vis[r]) {
                    continue;
                }
                vis[r] = true;
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
                pw.println(r + " " + q.size());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            assert pw != null;
            pw.close();
        }
    }
}

class DBpediaConnectedGraph {
    private WriteDBpedia gb;
    private List<String> idToNode; // rehash Integer to Entity Name
    private String graphName = null;
    private int id;

    DBpediaConnectedGraph(WriteDBpedia gb1, List<Integer> connected, String graphName1, int id1) {
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
     *
     * @param nodeMap   map nodes from origin graph to the component
     * @param connected connected nodes
     */
    void buildLucene(Map<Integer, Integer> nodeMap, List<Integer> connected) {
        try {
            String cgDir = Util.getLucenePath(graphName, id);
            //check if index is built
            Directory dir = FSDirectory.open(Paths.get(cgDir));
            if (DirectoryReader.indexExists(dir)) {  //if the index has been built, delete it
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
                for (String nodeLabel : gb.labelList.get(it)) {
                    sb.append(" ");
                    sb.append(nodeLabel);
                }
                if (sb.length() == 0) {
                    continue;
                }
                String nodeLabels = sb.substring(1, sb.length());

                Document document = new Document();
                document.add(new StringField("id", Integer.toString(u), Field.Store.YES));
                document.add(new TextField("label", nodeLabels, Field.Store.YES));
                indexWriter.addDocument(document);

                //pw.println(idToNode.get(u) + " " + u);
                //pw.println(nodeLabels);
            }
            indexWriter.forceMerge(1);
            indexWriter.close();
            dir.close();
            System.out.println(cgDir + " is built!");

            //pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}