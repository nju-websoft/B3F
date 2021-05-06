package graphdeal;

import graphdeal.database.MysqlReader;
import graphdeal.database.MysqlWriter;
import org.apache.jena.rdf.model.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.*;

/**
 * @Author Yuxuan Shi
 * @Date 11/26/2019
 * @Time 1:13 PM
 */
public class Util {
    /**
     * use sorted short to denote type
     * vaild if not USERDF2VEC
     */
    public static final boolean LABEL_SHORT = false;
    /**
     * use bit map to denote type
     * vaild if not USERDF2VEC
     */
    public static final boolean LABEL_BIT = !LABEL_SHORT;
    public static final String FIELDMAPINFO = "FieldMapInfo";
    /**
     * fold time threshold
     * even if it is set to a very small number, algorithms may repeat.
     */
    public static final long REPIME = 25 * 1000;

    public static void setCUTIME(long CUTIME) {
        Util.CUTIME = CUTIME;
    }

    /**
     * timeout
     */
    public static long CUTIME = 200 * 1000;
    /**
     * smallest number of nodes of a component
     */
    public static final int MINCOMPONENT = 100;

    public static final String LUBM20K = "lubm20k";
    public static final String LUBM100K = "lubm100k";
    public static final String LUBM2U = "lubm2U";
    public static final String LUBM10U = "lubm10U";
    public static final String LUBM50U = "lubm50U";
    public static final String DBPEDIA20K = "dbpedia20k";
    public static final String DBPEDIA50K = "dbpedia50k";
    public static final String DBPEDIA100K = "dbpedia100k";
    public static final String DBPEDIA500K = "dbpedia500k";
    public static final String DBPEDIA1000K = "dbpedia1000k";
    public static final int DBPEDIA20K_SIZE = (int) 2e4;
    public static final int DBPEDIA50K_SIZE = (int) 5e4;
    public static final int DBPEDIA100K_SIZE = (int) 1e5;
    public static final int DBPEDIA500K_SIZE = (int) 5e5;
    public static final int DBPEDIA1000K_SIZE = (int) 1e6;
    public static final String DBPEDIA = "dbpedia201610";
    public static final Boolean CUT_USA = false;
    public static String USA = "http://dbpedia.org/resource/United_States";
    /**
     * onestar's iteration
     */
    public static final byte MAXLOCALIT = 20;
    public static String DATA_DIR = "F:\\data\\ijcai\\";
    public static double ALPHA = 0.5;
    public static double BETA = 1D - ALPHA;
    /**
     * order class
     */
    public static Map<Class, Integer> CLASS_SORT;
    /**
     * diameter for banks
     */
    public static int DIAMETER = 10;
    /**
     * treesize b for ans
     */
    public static int TREEB = 6;
    /**
     * approximation ratio for b3f
     */
    public static double B3FACC = 1D;
    /**
     * rdf2vetor size
     */
    public static int RDFVEC = 10;

    public static void setUSERDF2VEC(boolean useRfd2Vec) {
        Util.USERDF2VEC = useRfd2Vec;
    }

    /**
     * true if user rdf2vec to compute sde
     */
    public static boolean USERDF2VEC = false;
    public static boolean CHECK_TIMEOUT = false;
    static String DBPEDIA_BASE = "F:\\data\\" + DBPEDIA + "\\";

    static {
        CLASS_SORT = new HashMap<>();
        CLASS_SORT.put(Integer.class, CLASS_SORT.size());
        CLASS_SORT.put(BigInteger.class, CLASS_SORT.size());
        CLASS_SORT.put(Double.class, CLASS_SORT.size());
        CLASS_SORT.put(BigDecimal.class, CLASS_SORT.size());
        CLASS_SORT.put(String.class, CLASS_SORT.size());
        CLASS_SORT.put(null, CLASS_SORT.size());
    }

    public static void setDataDir(String dataDir) {
        DATA_DIR = dataDir;
    }

    public static void setCheckTimeout(boolean checkTimeout) {
        CHECK_TIMEOUT = checkTimeout;
    }

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static String realFilePath(String path) {
        return path.replace("/", FILE_SEPARATOR).replace("\\", FILE_SEPARATOR);
    }

    public static String getHttpURLPath(String path) {
        return path.replace("\\", "/");
    }

    public static void writeJdbcPPS() {
        Properties pps = new Properties();
        pps.setProperty("JDBC_DRIVER", "com.mysql.jc.jdbc.Driver");
        pps.setProperty("DB_URL", "jdbc:mysql://localhost:3306/?user=root&characterEncoding=utf8&useCursorFetch=true");
        pps.setProperty("USER", "root");
        pps.setProperty("PASS", "anonymous");
        OutputStream out = null;
        try {
            out = new FileOutputStream(".\\src\\main\\resources\\jdbc.properties");
            pps.store(out, "Mysql information");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setAlpha(double a) {
        if (a < 0 || a > 1) {
            return;
        }
        ALPHA = a;
        BETA = 1D - ALPHA;
    }

    /**
     * write instance parameter
     */
    public static void writeInitPPS() {
        Properties pps = new Properties();
        pps.setProperty("INIT", "true");
        pps.setProperty("DATABASE", "mondial");
        pps.setProperty("GRAPH_NAME", "mondial");
        OutputStream out = null;
        try {
            out = new FileOutputStream(".\\src\\main\\resources\\instance.properties");
            pps.store(out, "Instance initial parameter");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * read instance parameter
     *
     * @return parameter json
     */
    public static Properties getInitPPS() {
        return getProperties("instance.properties");
    }

    public static Properties getProperties(@NotNull String basePath) {
        Properties pps = new Properties();
        try {
            InputStream in = Util.class.getClassLoader().getResourceAsStream(basePath);
            pps.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pps;
    }

    public static String getLuceneRoot(@NotNull String graphName) {
        String path = ".\\data\\Index\\" + graphName;;
        path = realFilePath(path);
        return path;
    }

    public static String getLucenePath(@NotNull String graphName, int id) {
        String path = ".\\data\\Index\\" + graphName + "\\component" + id;
        path = realFilePath(path);
        return path;
    }

    public static String getRdfTriple(String graphName) {
        String filePath = DATA_DIR + graphName;
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        filePath = realFilePath(filePath);
        String path = filePath + "\\" + "triple.txt";
        path = realFilePath(path);
        return path;
    }

    public static String getEntity(String graphName) {
        String path = DATA_DIR + graphName + "\\" + "entity.txt";
        path = realFilePath(path);
        return path;
    }

    public static String getRdf2Vec(String graphName) {
        String path = DATA_DIR + graphName + "\\" + "rdf2vec.txt";
        path = realFilePath(path);
        return path;
    }

    public static String getAnsJs(String graphName, String algorithm) {
        String filePath;
        if (Util.USERDF2VEC) {
            filePath = DATA_DIR + "result\\" + graphName + "_vec";
        } else {
            filePath = DATA_DIR + "result\\" + graphName + "_jac";
        }
        filePath = realFilePath(filePath);
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        String path = filePath + "\\" + Util.ALPHA + "." + algorithm + ".js";
        path = realFilePath(path);
        return path;
    }

    public static String getAnsTxt(String graphName, String algorithm) {
        String filePath;
        if (Util.USERDF2VEC) {
            filePath = DATA_DIR + "result\\" + graphName + "_vec";
        } else {
            filePath = DATA_DIR + "result\\" + graphName + "_jac";
        }
        filePath = realFilePath(filePath);
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        String path = filePath + "\\" + Util.ALPHA + "." + algorithm + ".txt";
        path = realFilePath(path);
        return path;
    }


    public static String getAnsJsWithAlpha(String graphName, String algorithm, double alp) {
        String filePath;
        if (Util.USERDF2VEC) {
            filePath = DATA_DIR + "result\\" + graphName + "_vec";
        } else {
            filePath = DATA_DIR + "result\\" + graphName + "_jac";
        }
        filePath = realFilePath(filePath);
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        return filePath + "\\" + alp + "." + algorithm + ".js";
    }

    public static String getAnsTxtWithAlpha(String graphName, String algorithm, double alp) {
        String filePath;
        if (Util.USERDF2VEC) {
            filePath = DATA_DIR + "result\\" + graphName + "_vec";
        } else {
            filePath = DATA_DIR + "result\\" + graphName + "_jac";
        }
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        String path = filePath + "\\" + alp + "." + algorithm + ".txt";
        path = realFilePath(path);
        return path;
    }

    public static String getNewTxt(String graphName, String algorithm) {
        String filePath;
        if (Util.USERDF2VEC) {
            filePath = DATA_DIR + "result\\" + graphName + "_vec";
        } else {
            filePath = DATA_DIR + "result\\" + graphName + "_jac";
        }
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        String path = filePath + "\\cikm." + algorithm + ".txt";
        path = realFilePath(path);
        return path;
    }

    public static String getOldAnsJs(String graphName, String algorithm) {
        String filePath = DATA_DIR + "result\\" + graphName;
        if (Util.USERDF2VEC) {
            filePath = filePath + "_vec";
        } else {
            filePath = filePath + "_jac";
        }
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        String path = filePath + "\\" + graphName + "." + algorithm + ".js";
        path = realFilePath(path);
        return path;
    }

    public static String getOldAnsTxt(String graphName, String algorithm) {
        String filePath = DATA_DIR + "result\\" + graphName;
        if (Util.USERDF2VEC) {
            filePath = filePath + "_vec";
        } else {
            filePath = filePath + "_jac";
        }
        File fp = new File(filePath);
        if (!fp.exists()) {
            fp.mkdirs();
        }
        String path = filePath + "\\" + graphName + "." + algorithm + ".txt";
        path = realFilePath(path);
        return path;
    }

    //equal or bigger
    public static boolean EqualBigger(double x, double y) {
        return x > y - 10e-8;
    }

    public static boolean RealBigger(double x, double y) {
        return x > y + 10e-8;
    }

    public static void bitBuild(int u, Map<Integer, List<Integer>> father, Map<Integer, BitSet> bitList) {
        if (bitList.containsKey(u)) {
            return;
        }
        BitSet bt = new BitSet(father.size());
        bt.set(u);
        bitList.put(u, bt);

        for (int fa : father.get(u)) {
            bitBuild(fa, father, bitList);
        }
        for (int fa : father.get(u)) {
            bitList.get(u).or(bitList.get(fa));
        }
    }

    public static void showRDF(String fileName, String goalFile) {
        String x, y, z;
        Model model = ModelFactory.createDefaultModel();
        model.read(fileName, "");
        //model.write(System.out,"TTL");

        Map<String, Integer> typeID = new TreeMap<>();
        Map<Integer, List<Integer>> father = new TreeMap<>();
        Map<Integer, BitSet> bitList = new TreeMap<>();

        ResIterator subjects = model.listSubjects();

        //PrintWriter pw = new PrintWriter(goalFile);
        while (subjects.hasNext()) {
            Resource subject = subjects.next();
            // get all triples
            StmtIterator properties = subject.listProperties();
            while (properties.hasNext()) {
                Statement stmtS = properties.nextStatement();
                Property predicate = stmtS.getPredicate();
                RDFNode object = stmtS.getObject();
                x = subject.toString();
                y = predicate.toString();
                z = object.toString();
                if (predicate.getLocalName().equals("subClassOf")) {
                    if (object.isURIResource()) {
                        if (!typeID.containsKey(x)) {
                            typeID.put(x, typeID.size());
                        }
                        if (!typeID.containsKey(z)) {
                            typeID.put(z, typeID.size());
                        }
                        int u = typeID.get(x);
                        int v = typeID.get(z);
                        if (!father.containsKey(u)) {
                            father.put(u, new ArrayList<>());
                        }
                        if (!father.containsKey(v)) {
                            father.put(v, new ArrayList<>());
                        }
                        father.get(u).add(v);
                    }
                }
            }
        }
        for (int u : father.keySet()) {
            bitBuild(u, father, bitList);
            System.out.println(u + " " + bitList.get(u));
        }
    }

    /**
     * write the vector to databse db
     *
     * @param db graph to be written
     */
    public static void writeVector(String db) {
        try (MysqlReader mr = new MysqlReader(db);
             Scanner sc = new Scanner(new File(getRdf2Vec(db)))) {
            //get node id
            mr.dbInit("nodeID");
            List<Object> res = null;
            Map<String, Integer> nodeId = new HashMap<>();
            while ((res = mr.readNodeID()) != null) {
                nodeId.put((String) res.get(0), (Integer) res.get(1));
            }

            List<List<Double>> vectorList = new ArrayList<>();
            for (int i = 0; i < nodeId.size(); i++) {
                vectorList.add(new ArrayList<>());
            }
            while (sc.hasNext()) {
                String line = sc.nextLine();
                String[] sts = line.split(" ");
                if (sts[0] == null || !nodeId.containsKey(sts[0])) {
                    System.out.println(line);
                }
                List<Double> vect = vectorList.get(nodeId.get(sts[0]));
                for (int i = 1; i < sts.length; i++) {
                    vect.add(Double.parseDouble(sts[i]));
                }
            }
            MysqlWriter mw = new MysqlWriter(db);
            mw.dbInit("rdf2vec");
            for (int i = 0; i < vectorList.size(); i++) {
                mw.insertRdf2Vec(i, vectorList.get(i));
            }
            mw.writeEnd();
            mw.close();
        } catch (SQLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
