package experiment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import dpbf.DPBFNodeBase;
import dpbf.GraphDPBF;
import b3f.B3F;
import b3f.GraphB3F;
import graphdeal.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @Author Yuxuan Shi
 * @Date 12/11/2019
 * @Time 4:45 PM
 * run lubm dataset
 */
public class TestAllLubmNode {
    List<List<String>> words;
    Map<String, List<Integer>> wordsMap;

    void inputquery(String fileName, String mapName) throws IOException {
        words = new ArrayList<>();
        Scanner input1 = new Scanner(new File(fileName));
        while (input1.hasNext()) {
            String line = input1.nextLine();
            words.add(Arrays.asList(line.split(";")));
        }
        input1.close();

        wordsMap = new HashMap<>();
        Scanner input2 = new Scanner(new File(mapName));
        while (input2.hasNext()) {
            String line = input2.nextLine();
            String[] newwords = line.split(":");
            String [] mapped = newwords[1].split(",");
            List<Integer> mi = new ArrayList<>();
            for (String it : mapped) {
                mi.add(Integer.parseInt(it));
            }
            wordsMap.put(newwords[0], mi);
        }
        input2.close();

    }

    void outputans(String st, String fileName, List<QueryInfo> queryInfos) throws IOException {
        PrintWriter fop = new PrintWriter(
                Util.getAnsTxt(st, fileName));

        double tTime = 0, tWeight = 0, tSal = 0D, tCoh = 0D;
        for (QueryInfo queryInfo : queryInfos) {
            queryInfo.calcMid();
            fop.println(queryInfo.queryString());
            fop.println("time: "+ queryInfo.getTime() + " ms");
            fop.println("weight: "+ queryInfo.getScore() + " " + queryInfo.getSal() + " " + queryInfo.getCoh());

            tTime += queryInfo.getTime();
            tWeight += queryInfo.getScore();
            tSal += queryInfo.getSal();
            tCoh += queryInfo.getCoh();
        }
        tTime = tTime/queryInfos.size();
        tWeight = tWeight/queryInfos.size();
        tSal = tSal / queryInfos.size();
        tCoh = tCoh / queryInfos.size();
        fop.println("avg time: "+ tTime  + " ms");
        fop.println("avg weight: "+ tWeight + " " + tSal + " " + tCoh);
        fop.close();
        //storeJson(st, fileName, queryInfos);
    }

    void storeJson(String st, String fileName, List<QueryInfo> queryInfos) throws IOException {
        PrintWriter fop = new PrintWriter(
                new File(Util.getAnsJs(st, fileName)));
        SimplePropertyPreFilter filter = new SimplePropertyPreFilter(AnsTree.class, "nodes", "edges", "score");
        for (QueryInfo queryinfo : queryInfos)
            fop.println(JSON.toJSONString(queryinfo.getAns(), filter));
        fop.close();
    }

    void search(SearchBase sb, GraphBase gb, int testTime, int iTime, String graph, String alg){
        try {
            //delete the file if they already exist
            File fileTxt = new File(Util.getAnsTxt(graph, alg));
            if (fileTxt.exists()) {
                fileTxt.delete();
            }
            File fileJs = new File(Util.getAnsJs(graph, alg));
            if (fileJs.exists()) {
                fileJs.delete();
            }

            long startTime, endTime;
            List<QueryInfo> infos = new ArrayList<>();
            for (List<String> word : words)
                infos.add(new QueryInfo(word, alg));
            for (int tt = 0; tt < testTime; tt++) {
                //record the avg time and weight
                double tTime = 0D, tWeight = 0D, tSal = 0D, tCoh = 0D;
                for (int i = 0; i < words.size(); i++) {
                //for (int i = 0; i < 2; i++) {
                    QueryInfo info = infos.get(i);

                    //repeat if it costs smaller than the repeat time
                    if (info.getFirstTime() < Util.REPIME) {
                        startTime = System.currentTimeMillis();
                        int itCnt = 0;
                        for (int j = 0; j < iTime; j++) {
                            itCnt++;
                            //ignore the exception of search
                            try {
                                sb.search(gb, words.get(i), wordsMap);
                            }catch (Exception ignored) {
                            }

                            if (!sb.isRepeatFlag()) {
                                break;
                            }
                        }
                        endTime = System.currentTimeMillis();
                        info.addTime((double) (endTime - startTime) / itCnt);
                        //store the answer tree if it is null
                        if (info.getAns() == null)
                            info.setAns(sb.getAnsTree());
                        if (info.getAns() == null) {
                            System.out.println(tt + " " + i + " No answer!" +
                                    " " + (double) (endTime - startTime) / itCnt);
                        } else {
                            System.out.println(tt + " " + i + " " + info.getAns().getScore() +
                                    " " + info.getSal() + " " + info.getCoh() +
                                    " " + (double) (endTime - startTime) / itCnt);
                        }
                    }
                    if (tt == 0) {
                        PrintWriter foptxt = new PrintWriter(
                                new FileWriter(Util.getAnsTxt(graph, alg), true));
                        foptxt.println(info.queryString());
                        foptxt.println("time: " + info.getFirstTime() + " ms");
                        foptxt.println("weight: " + info.getScore() + " " + info.getSal() + " " + info.getCoh());
                        foptxt.close();

                        tTime += info.getFirstTime();
                        tWeight += info.getScore();
                        tSal += info.getSal();
                        tCoh += info.getCoh();

                        //record the json
                        PrintWriter fop = new PrintWriter(
                                new FileWriter(Util.getAnsJs(graph, alg), true));
                        SimplePropertyPreFilter filter = new SimplePropertyPreFilter(AnsTree.class, "nodes", "edges", "score");
                        if (info.getAns() != null) {
                            fop.println(JSON.toJSONString(info.getAns(), filter));
                        } else {
                            fop.println();
                        }
                        fop.close();
                    }
                }

                if (tt == 0) {
                    PrintWriter foptxt = new PrintWriter(
                            new FileWriter(Util.getAnsTxt(graph, alg), true));
                    tTime = tTime / infos.size();
                    tWeight = tWeight / infos.size();
                    tSal = tSal / infos.size();
                    tCoh = tCoh / infos.size();
                    foptxt.println("avg time: " + tTime + " ms");
                    foptxt.println("avg weight: " + tWeight + " " + tSal + " " + tCoh);
                    foptxt.close();
                }
            }
            outputans(graph, alg, infos);
        } catch (Exception e) {
        }
    }

    void dealWithDPBF(String st){
        Util.setAlpha(0.5);
        search(DPBFNodeBase.getInstance(), GraphDPBF.getInstance(), 3, 3, st, "DPBFBase");
        TestAllGivenNode.wirteOther("DPBFBase");
        DPBFNodeBase.closeInstance();
        GraphDPBF.closeInstance();
    }

    void dealWithB3F(String st) {
        double []als = new double[]{0.3, 0.5, 0.7};
        for (double al : als) {
            Util.setAlpha(al);
            search(B3F.getInstance(), GraphB3F.getInstance(), 3, 3, st, "b3f");
        }
        B3F.closeInstance();
        GraphB3F.closeInstance();
    }

    void testDeal() throws Exception {

        Properties pps = Util.getInitPPS();
        String st = pps.get("GRAPH_NAME").toString();
        if (!st.startsWith("lubm")) {
            throw new Exception("should be a lubm graph");
        }
        Util.setUSERDF2VEC(true);
        String queryFile = Util.DATA_DIR + "query\\" + st + "\\query.txt";
        String queryMap = Util.DATA_DIR + "query\\" + st + "\\querymap.txt";
        inputquery(queryFile, queryMap);


        try {
            //dealWithDPBF(st);
            dealWithB3F(st);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            Runtime.getRuntime().gc();
        }
        finally {
        }
    }

    public static void main(String[] args) throws Exception {
        TestAllLubmNode t1 = new TestAllLubmNode();
        t1.testDeal();
    }
}

