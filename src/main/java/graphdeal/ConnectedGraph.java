package graphdeal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;

/**
 * @Author Yuxuan Shi
 * @Date 11/10/2019
 * @Time 8:53 PM
 */
public class ConnectedGraph{
    private GraphBase gb;
    private int nodeNum;
    private List<List<HopVDE>> graph; // read the graph
    private Map<String, List<Integer>> invTable; // keyword inverted table
    private List<String> idToNode; // rehash Integer to Entity Name
    private List<String> idToEdge;
    private List<List<Short>> typeList;	//type of any node
    private List<BitSet> typeBit;
    private List<Double> nodeWeight;	// node weight
    private List<List<Double>> rdf2vec;
    private List<Double> rdf2norm;
    private String graphName;
    private int id;
    private IndexSearcher iSearcher = null;
    private DirectoryReader iReader = null;
    private Set<Integer> luceneScore = null;
    private Map<String, Class> luceneFieldMap;


    boolean isLubm() {
        if (graphName.startsWith("lubm")) {
            return true;
        }
        return false;
    }
    //connect contains all nodes in a connected componment and can't appear for more than once

    /**
     * generate connected graph
     * @param gb1 graph base
     * @param connected connected nodes
     * @param graphName1 name of graph
     * @param id1 id of the connected graph
     */
    ConnectedGraph(GraphBase gb1, List<Integer> connected,
                   String graphName1, int id1){
        gb = gb1;
        graphName = graphName1;
        id = id1;
        nodeNum = connected.size();
        graph = new ArrayList<>(connected.size());

        //generate rehash table
        Map<Integer, Integer> nodeMap = new TreeMap<>();
        idToNode = new ArrayList<>(connected.size());
        for (int it : connected) {
        //connected must have exactly the same order otherwise it will be wrong!
            nodeMap.put(it, idToNode.size());
            idToNode.add(gb.idToNode.get(it));
        }

        idToEdge = gb.idToEdge;
        //generate graph
        for (int it : connected){
            List<HopVDE> hp = new ArrayList<>();
            //change the edge

            //cut usa
            if (Util.CUT_USA) {
                if (Util.USA.equals(idToNode.get(it))) {
                    graph.add(hp);
                    continue;
                }
            }

            for (HopVDE hop : gb1.graph.get(it)){
                int v = nodeMap.get(hop.v);
                //cut usa
                if (Util.CUT_USA) {
                    if (Util.USA.equals(idToNode.get(v))) {
                        continue;
                    }
                }
                hp.add(new HopVDE(v, hop.getDis(), hop.edgeID));
            }
            graph.add(hp);
        }

        //sort edges for further use
        for (List<HopVDE> lhop : graph) {
            lhop.sort(Comparator.comparingInt(x -> x.v));
        }

        //read for sd compute
        if (!Util.USERDF2VEC) {
            //generate type list
            if (Util.LABEL_SHORT) {
                typeList = new ArrayList<>(connected.size());
                for (int it : connected) {
                    typeList.add(gb.typeList.get(it));
                }
            }
            if (Util.LABEL_BIT) {
                typeBit = new ArrayList<>(connected.size());
                for (int it : connected) {
                    BitSet bt = new BitSet(gb.typeID.size());
                    for (short x : gb.typeList.get(it)) {
                        bt.set(x);
                    }
                    typeBit.add(bt);
                }
            }
        }
        else {
            rdf2vec = new ArrayList<>(connected.size());
            rdf2norm = new ArrayList<>(connected.size());
            for (int it : connected) {
                rdf2vec.add(gb.rdf2vec.get(it));
                double norm = 0D;
                for (double dim : gb.rdf2vec.get(it)) {
                    norm = norm + dim * dim;
                }
                norm = Math.sqrt(norm);
                rdf2norm.add(norm);
            }
        }

        luceneFieldMap = new TreeMap<>();

        // count type numbers
        /*Map<Integer, Integer> typeCnt = new TreeMap<>();
        for (List<Integer> it : typeList) {
            if (!typeCnt.containsKey(it.size())) typeCnt.put(it.size(), 1);
            typeCnt.put(it.size(), typeCnt.get(it.size())+1);
        }
        for (Map.Entry<Integer, Integer> it : typeCnt.entrySet())
            System.out.println(it.getKey() + " " + it.getValue());*/

        //read lucene index if not lubm
        if (!isLubm()) {
            buildLucene(nodeMap, connected);
            readLuence();
        }
        //generate node weight
        nodeWeight = new ArrayList<>(connected.size());
        for (int it : connected) {
            nodeWeight.add(gb.nodeWeight.get(it));
        }

        if (Util.CUT_USA) {
            setConnected();
        }

    }

    boolean[] connected;
    void setConnected() {
        int maxLoc = -1;
        int maxVal = -1;
        boolean[] visited = new boolean[nodeNum];
        Arrays.fill(visited, false);
        for (int i = 0; i < nodeNum;i ++) {
            if (visited[i]) {
                continue;
            }
            Queue<Integer> q = new LinkedList<>();
            q.add(i);
            visited[i] = true;
            int cnt = 0;
            while (!q.isEmpty()) {
                int vv = q.poll();
                cnt++;
                for (HopVDE lhop : graph.get(vv)) {
                    if (visited[lhop.v]) {
                        continue;
                    }
                    visited[lhop.v] = true;
                    q.add(lhop.v);
                }
            }
            if (cnt > maxVal) {
                maxLoc = i;
                maxVal = cnt;
            }
        }

        connected = new boolean[nodeNum];
        Arrays.fill(connected, false);
        {
            Queue<Integer> q = new LinkedList<>();
            q.add(maxLoc);
            connected[maxLoc] = true;
            while (!q.isEmpty()) {
                int vv = q.poll();
                for (HopVDE lhop : graph.get(vv)) {
                    if (connected[lhop.v]) {
                        continue;
                    }
                    connected[lhop.v] = true;
                    q.add(lhop.v);
                }
            }
        }
    }
    //build Lucene index
    void buildLucene(Map<Integer, Integer> nodeMap, List<Integer> connected){
        try {
            String cgDir = Util.getLucenePath(graphName, id);
            //check if index is built
            Directory dir = FSDirectory.open(Paths.get(cgDir));

            if (DirectoryReader.indexExists(dir)){  //if the index has been built
                System.out.println(cgDir + " has been built!");
                dir.close();
                return;
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

    void parseField(String luceneFieldName, String luceneFieldType){
        if (luceneFieldType.equals(Integer.class.toString())){
            luceneFieldMap.put(luceneFieldName, Integer.class);
            return;
        }
        if (luceneFieldType.equals(Double.class.toString())){
            luceneFieldMap.put(luceneFieldName, Double.class);
            return;
        }
        if (luceneFieldType.equals(String.class.toString())){
            luceneFieldMap.put(luceneFieldName, String.class);
            return;
        }
        System.out.println("Lucene index is not right!");
        return;
    }

    Query parseKeyword(String st) throws ParseException, NoSuchMethodException {
        Query query = null;
        int loc = st.indexOf(':');
        if (loc != -1){
            String typeName = st.substring(0, loc);
            if (!luceneFieldMap.containsKey(typeName)) return query;
            String sentence = st.substring(loc + 1);
            Class typeClass = luceneFieldMap.get(typeName);

            //number range search
            if (Number.class.isAssignableFrom(typeClass)){
                Method m1 = null;
                Object lowValue = null, highValue = null;
                if (typeClass.equals(Integer.class)) {
                    m1 = Integer.class.getDeclaredMethod("parseInt", String.class);
                    lowValue = Integer.MIN_VALUE;
                    highValue = Integer.MAX_VALUE;
                }
                if (typeClass.equals(Double.class)) {
                    m1 = Double.class.getDeclaredMethod("parseDouble", String.class);
                    lowValue = Double.MIN_VALUE;
                    highValue = Double.MAX_VALUE;
                }
                String endLoc = null;
                String startLoc = null;
                try {
                    if (sentence.charAt(0)=='<') {
                        endLoc = sentence.substring(1);
                    }
                    if (sentence.charAt(0)=='>') {
                        startLoc = sentence.substring(1);
                    }
                    int splitLoc = sentence.indexOf('~');
                    if (splitLoc != -1){
                        startLoc = sentence.substring(0, splitLoc);
                        endLoc = sentence.substring(splitLoc + 1);
                    }
                    try {
                        assert m1 != null;
                        if (startLoc != null) {
                            lowValue = m1.invoke(null, startLoc);
                        }
                        if (endLoc != null) {
                            highValue = m1.invoke(null, endLoc);
                        }
                    } catch(NumberFormatException e1){
                        System.out.println(startLoc + " or " + endLoc
                                + " is not " + typeClass.getSimpleName());
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (typeClass.equals(Integer.class)){
                    query = IntPoint.newRangeQuery(typeName, (int)lowValue, (int)highValue);
                    return query;
                }
                if (typeClass.equals(Double.class)){
                    query = DoublePoint.newRangeQuery(typeName, (double)lowValue, (double)highValue);
                    return query;
                }
            }

            if (String.class.equals(typeClass)){
                QueryParser qp = new QueryParser("label", new StandardAnalyzer());
                query = qp.parse(sentence.replaceAll(" "," && "));
            }
        }
        else {
            QueryParser qp = new QueryParser("label", new StandardAnalyzer());
            query = qp.parse(st.replaceAll(" ", " && "));
        }
        return query;
    }

    //read the index to memory
    void readLuence(){
        try {
            String cgDir = Util.getLucenePath(graphName, id);
            Directory dir = FSDirectory.open(Paths.get(cgDir));
            iReader = DirectoryReader.open(dir);
            iSearcher = new IndexSearcher(iReader);
            //get all fields
            QueryParser qp = new QueryParser(Util.FIELDMAPINFO, new StandardAnalyzer());
            TopDocs hits = iSearcher.search(qp.parse(Util.FIELDMAPINFO), 1);
            for (ScoreDoc hit : hits.scoreDocs) {
                Document hitDoc = iSearcher.doc(hit.doc);
                List<IndexableField> fields = hitDoc.getFields();
                for (IndexableField field : fields) {
                    parseField(field.name(), hitDoc.get(field.name()));
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

    }

    public void cntRadius() {
        int []cDist = new int[nodeNum];
        Arrays.fill(cDist, Integer.MAX_VALUE);
        LinkedList<Integer> q = new LinkedList<>();
        q.push(0);
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
        System.out.println(rad);
    }


    void givenQueryWord(List<String> keywords, Map<String, List<Integer>> keywordsMap) {
        invTable = keywordsMap;
    }

    /**
     * use lucene to get the map of each keyword
     * @param keywords keywords to be searched
     */
    void givenQueryWord(List<String> keywords) throws Exception{
        if (invTable != null) {
            invTable.clear();
        }

        if (isLubm()) {
            throw new Exception("Lubm don't have lucene!");
        }
        invTable = new TreeMap<>();

        try {
            //translate all keywords to queries
            List<Query> queryList = new ArrayList<>();
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (String st : keywords) {
                Query query = parseKeyword(st);
                queryList.add(query);
                builder.add(query, BooleanClause.Occur.SHOULD);
            }
            BooleanQuery bq = builder.build();
            TopDocs queryOR = iSearcher.search(bq, Integer.MAX_VALUE);

            //put score in nodeWeight
            /*if (luceneScore != null){
                for (int u : luceneScore) {
                    nodeWeight.set(u, 1D);
                }
                luceneScore.clear();
            }
            luceneScore = new TreeSet<>();
            double maxS = queryOR.getMaxScore();
            for (ScoreDoc hit : queryOR.scoreDocs) {
                Document hitDoc = iSearcher.doc(hit.doc);
                luceneScore.add(Integer.parseInt(hitDoc.get("id")));
                nodeWeight.set(Integer.parseInt(hitDoc.get("id")), 1 - hit.score/maxS);
            }*/

            for (int i = 0; i < keywords.size(); i++) {
                String st = keywords.get(i);
                //Query query = parseKeyword(st);
                Query query = queryList.get(i);
                TopDocs hits = iSearcher.search(query, Integer.MAX_VALUE);
                List<Integer> keyInts = new ArrayList<>((int) hits.totalHits);
                for (ScoreDoc hit : hits.scoreDocs) {
                    Document hitDoc = iSearcher.doc(hit.doc);
                    /*for (IndexableField field : hitDoc.getFields())
                        System.out.print(field.name() + ":" + hitDoc.get(field.name())+ "\"");
                    System.out.println();*/
                    keyInts.add(Integer.parseInt(hitDoc.get("id")));
                }
                if (keyInts.size() == 0) {
                    continue;
                }
                //System.out.println(keyInts);
                invTable.put(st, keyInts);
            }
            //dir.close();
        } catch (IOException | ParseException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    boolean containQueryWord(String st){
        try {
            Query query = parseKeyword(st);
            TopDocs hits = iSearcher.search(query, 1);
            if (hits.totalHits == 0) {
                return false;
            }
            if (Util.CUT_USA) {
                for (ScoreDoc hit : hits.scoreDocs) {
                    Document hitDoc = iSearcher.doc(hit.doc);
                    if (connected[Integer.parseInt(hitDoc.get("id"))]) {
                        return true;
                    }
                }
                return false;
            }
        } catch (ParseException | NoSuchMethodException | IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    /*void givenQueryWord(List<String> keywords){
        if (invTable != null) invTable.clear();
        invTable = new TreeMap<>();
        try {
            for (String st : keywords) {
                Query query = parser.parse(st.replaceAll(" ", " && "));
                ScoreDoc[] hits = iSearcher.search(query,Integer.MAX_VALUE).scoreDocs;
                List<Integer> keyInts = new ArrayList<>(hits.length);
                // Iterate through the results:
                for (ScoreDoc hit : hits) {
                    Document hitDoc = iSearcher.doc(hit.doc);
                    System.out.println(hitDoc.get("id"));
                    System.out.println(hitDoc.get("label"));
                    keyInts.add(Integer.parseInt(hitDoc.get("id")));
                }
                if (keyInts.size() == 0) continue;
                System.out.println(keyInts);
                invTable.put(st, keyInts);
            }
            //dir.close();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }*/

    public List<Integer> getKeynode(String keyword){
        if (!invTable.containsKey(keyword)) {
            return null;
        }
        return invTable.get(keyword);

    }

    public double getKeyWordInfo(List<String> keywords) {
        double sum = 0;
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (String st : keywords) {
            sum = sum + invTable.get(st).size();
            max = Math.max(max, invTable.get(st).size());
            min = Math.min(min, invTable.get(st).size());
        }
        sum = sum /  keywords.size();
        System.out.println(sum + " " + min + " " + max);
        return sum;
    }
    /**
     * check if a tree covers all keywords
     * @param ans1 an answer tree
     * @param keywords keyword list
     * @return true if ans1 covers all keywords
     */
    public boolean checkTreeCover(AnsTree ans1, List<String> keywords){
        if (ans1 == null) {
            return false;
        }
        for (String st : keywords){
            boolean contain = false;
            for (int u : getKeynode(st)) {
                if (ans1.nodes.contains(u)) { contain = true; }
            }
            if (!contain) { return false; }
        }
        return true;
    }

    public int getNodeNum(){
        return nodeNum;
    }

    public void addNodeWeight(double we){
        nodeWeight.add(we);
    }
    public void removeNodeWeight(){
        nodeWeight.remove(nodeWeight.size() - 1);
    }
    public double getNodeWeight(int u){
        return nodeWeight.get(u);
    }

    public int binarySearch(int u, int v){
        List<HopVDE> list = graph.get(u);
        int low = 0;
        int high = list.size()-1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = list.get(mid).v - v;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }

    public int binarySearchID(int u, int v){
        int goal = binarySearch(u, v);
        if (goal < 0) {
            return -1;
        }
        return graph.get(u).get(goal).edgeID;
    }

    public String getIDToNode(int u){ return idToNode.get(u); }

    public String getIDToEdge(int uv){ return idToEdge.get(uv); }

    public List<HopVDE> getEdges(int u) { return graph.get(u);};

    //public List<Short> getTypeList(int u) { return typeList.get(u);}

    public List<List<HopVDE>> getGraph(){ return graph;};

    /**
     *
     * @param keywords keywords list
     * @return true if the graph contains all keywords
     */
    public boolean keyContains(List<String> keywords){
        for (String it : keywords) {
            if (getKeynode(it) == null) {
                System.out.println("graph doesn't contain " + it);
                return false;
            }
        }
        return true;
    }

    /**
     * calculate pairwise sde using bit set
     * @param u a node
     * @param v a node
     * @return sde(u,v)
     */
    private double sdeLB(int u, int v) {
        int tt = typeBit.get(u).cardinality() + typeBit.get(v).cardinality();
        if (tt == 0) {
            return 1;
        }
        BitSet bt = (BitSet)typeBit.get(u).clone();
        bt.and(typeBit.get(v));
        int mix = bt.cardinality();
        return 1-(double)mix/(tt-mix);
    }

    /**
     * calculate pairwise sde using set union
     * @param u a node
     * @param v a node
     * @return sde(u,v)
     */
    private double sdeLS(int u, int v) {
        int tt = typeList.get(u).size() + typeList.get(v).size();
        if (tt == 0) {
            return 1;
        }
        int mix = 0;
        int ul = 0, vl = 0;
        while (ul < typeList.get(u).size() && vl < typeList.get(v).size()) {
            if (typeList.get(u).get(ul) < typeList.get(v).get(vl)) {
                ul++;
                continue;
            }
            if (typeList.get(u).get(ul) > typeList.get(v).get(vl)) {
                vl++;
                continue;
            }
            //now we have equal type
            ul++;
            vl++;
            mix++;
        }
        return 1 - (double) mix / (tt - mix);
    }

    /**
     * calculate pairwise sde using angular distance
     * @param u a node
     * @param v a node
     * @return sde(u,v)
     */
    private double sdeAD(int u, int v) {
        double duv = 0;
        for (int i = 0; i < rdf2vec.get(u).size(); i++) {
            duv = duv + rdf2vec.get(u).get(i) * rdf2vec.get(v).get(i);
        }
        duv = duv / rdf2norm.get(u) / rdf2norm.get(v);
        return Math.acos(duv) / Math.PI;
    }
    /**
     * calculate pairwise sde
     * @param u a node
     * @param v a node
     * @return sde(u,v)
     */
    public double sde(int u, int v) {
        if (u == v) {
            return 0;
        }
        if (!Util.USERDF2VEC) {
            if (Util.LABEL_SHORT) {
                return sdeLS(u,v);
            }
            if (Util.LABEL_BIT) {
                return sdeLB(u, v);
            }
        }
        return sdeAD(u, v);
    }

    int cachedRoot = -1;
    Map<Integer, Double> cachedRootSD;
    /**
     * calculate cached pairwise sde
     * @param u a node
     * @param v a node
     * @return sde(u,v)
     */
    public double cachedSde(int u, int v) {
        if (u == v) {
            return 0;
        }
        if (cachedRoot != u) {
            cachedRoot = u;
            cachedRootSD = new TreeMap<>();
        }
        if (!cachedRootSD.containsKey(v)) {
            cachedRootSD.put(v, sde(u, v));
        }
        return cachedRootSD.get(v);
    }

    void close(){
        try {
            for (List<HopVDE> it : graph) {
                it.clear();
            }
            graph.clear();
            if (invTable != null) {
                invTable = null;
            }
            idToNode.clear();
            if (!Util.USERDF2VEC) {
                if (Util.LABEL_SHORT) {
                    for (List<Short> it : typeList) {
                        it.clear();
                    }
                    typeList.clear();
                }
                if (Util.LABEL_BIT) {
                    for (BitSet it : typeBit) {
                        it.clear();
                    }
                    typeBit.clear();
                }
            }
            if (Util.USERDF2VEC) {
                for (List<Double> it : rdf2vec) {
                    it.clear();
                }
                rdf2vec.clear();
            }
            nodeWeight.clear();
            iReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}