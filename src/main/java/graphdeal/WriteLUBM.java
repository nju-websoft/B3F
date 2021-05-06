package graphdeal;

import graphdeal.database.MysqlWriter;
import org.apache.jena.rdf.model.*;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.*;

/**
 * write lubm to database
 *
 * @Author Yuxuan Shi
 * @date 2020/5/21
 */
public class WriteLUBM extends GraphBase{

    @Override
    protected void graphInit() {
    }

    private boolean writeCheck(String graphName){
        System.out.println("Danger! this function will overwrite " + graphName + " dataset. Continue(Y/N)?");
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

    public void writeAll(String graphName) throws IOException {

        if (!writeCheck(graphName)) {
            return;
        }
        try {

            MysqlWriter mw = new MysqlWriter(graphName);
            mw.dbInit("graphTriple");
            Model model = ModelFactory.createDefaultModel();
            String graphFile = Util.DATA_DIR + graphName + "\\" + "lubm.n3";
            model.read(graphFile);
            StmtIterator iter = model.listStatements();
            List<List<String>> rdfTriple = new ArrayList<>();
            String x, y, z;
            while (iter.hasNext()) {
                Statement stmtS = iter.nextStatement();
                Resource subject = stmtS.getSubject();
                Property predicate = stmtS.getPredicate();
                RDFNode object = stmtS.getObject();

                if (object.isResource()) {
                    x = subject.toString();
                    y = predicate.toString();
                    z = object.toString();
                    rdfTriple.add(Arrays.asList(x, y, z));
                    mw.insertGraphTriple(x, y, z);

                    int u = tryAddNode(x);
                    int v = tryAddNode(z);
                    int uv = tryAddEdge(y);
                    this.graph.get(u).add(new HopVDE(v, 1D, uv));
                    this.graph.get(v).add(new HopVDE(u, 1D, uv));
                }
            }
            model.close();
            mw.writeEnd();

            PrintWriter pw;
            //write rdf triple

            pw = new PrintWriter(Util.getRdfTriple(graphName));
            for (List<String> triple : rdfTriple) {
                pw.println(triple.get(0) + " " + triple.get(1) + " " + triple.get(2));
            }
            pw.close();
            pw = new PrintWriter(Util.getEntity(graphName));
            for (String st : nodeID.keySet()) {
                pw.println(st);
            }
            pw.close();

            /*int []cDist = new int[nodeID.size()];
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
            System.out.println("radius" + rad);
            rad =0;
            for (int i = 0; i < cDist.length; i++) {
                if (cDist[i] < Integer.MAX_VALUE) rad++;
            }
            System.out.println(rad);*/
            //if (true) return;

            //get page rank
            Graph<Integer, DefaultEdge> jGraph = new DefaultUndirectedGraph<>(DefaultEdge.class);
            for (int i = 0; i < this.graph.size(); i++) {
                jGraph.addVertex(i);
            }
            for (int u = 0; u < this.graph.size(); u++) {
                for (HopVDE it : this.graph.get(u)) {
                    jGraph.addEdge(u, it.v);
                }
            }
            PageRank<Integer, DefaultEdge> pageRank = new PageRank<>(jGraph);

            double maxPR = 0;
            double minPR = Double.MAX_VALUE;
            for (int i = 0; i < this.graph.size(); i++) {
                maxPR = Math.max(maxPR, pageRank.getVertexScore(i));
                minPR = Math.min(minPR, pageRank.getVertexScore(i));
            }

            //wirte nodeID
            mw.dbInit("nodeID");
            for (Map.Entry<String, Integer> it : nodeID.entrySet()) {
                double pr = 0D;
                pr = Math.log10(pageRank.getVertexScore(it.getValue())) - Math.log10(minPR);
                pr = 1 - 1 / (1 + Math.exp((-pr)));
                mw.insertNodeID(it.getKey(), it.getValue(), pr);
            }
            mw.writeEnd();
            //write type triple
            mw.dbInit("typeTriple");
            mw.writeEnd();

            //write typeID
            mw.dbInit("typeID");
            mw.writeEnd();

            //write type
            mw.dbInit("type");
            mw.writeEnd();

            close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * test whether the graph is fully connected or not
     * @param graphName
     */
    public void checkConnection(String graphName) {
        Model model = ModelFactory.createDefaultModel();
        String graphFile = Util.DATA_DIR + graphName + "\\" + "lubm.n3";
        model.read(graphFile);
        StmtIterator iter = model.listStatements();
        String x, y, z;
        while (iter.hasNext()) {
            Statement stmtS = iter.nextStatement();
            Resource subject = stmtS.getSubject();
            Property predicate = stmtS.getPredicate();
            RDFNode object = stmtS.getObject();

            if (object.isResource()) {
                x = subject.toString();
                y = predicate.toString();
                z = object.toString();
                int u = tryAddNode(x);
                int v = tryAddNode(z);
                int uv = tryAddEdge(y);
                this.graph.get(u).add(new HopVDE(v, 1D, uv));
                this.graph.get(v).add(new HopVDE(u, 1D, uv));
            }
        }
        model.close();

        Boolean visited[] = new Boolean[graph.size()];
        Arrays.fill(visited, false);
        visited[0] = true;
        LinkedList<Integer> q = new LinkedList<>();
        q.push(0);
        int cnt = 0;
        while (!q.isEmpty()) {
            int u = q.poll();
            cnt++;
            for (HopVDE it : graph.get(u)) {
                int v = it.v;
                if (!visited[v]) {
                    visited[v] = true;
                    q.offerLast(v);
                }
            }
        }
        System.out.println(graph.size() +  " " + cnt);
    }
}
