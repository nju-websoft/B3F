package experiment;

import graphdeal.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * @Author Yuxuan Shi
 * @Date 12/18/2019
 * @Time 9:53 AM
 * this is the class to analyse experimental results
 */
public class AnsSummary {

    /**
     * read all answers from ans txt
     *
     * @param graphName the graph used
     * @param algorithm the algorithm used
     * @throws FileNotFoundException
     */
    public static List<QueryInfo> readAnsTxt(String graphName, String algorithm) throws FileNotFoundException {
        List<QueryInfo> ansList = new ArrayList<>();
        Scanner sc = new Scanner(new File(Util.getAnsTxt(graphName, algorithm)));
        while (sc.hasNext()) {
            String line = sc.nextLine();
            if (line.contains("avg time:")) {
                break;
            }
            List<String> query = new ArrayList<>(Arrays.asList(line.split(";")));
            QueryInfo queryInfo = new QueryInfo(line, query, algorithm);
            line = sc.nextLine();
            line = line.substring(line.indexOf(' ') + 1, line.lastIndexOf(' '));
            queryInfo.setTime(Double.parseDouble(line));

            line = sc.nextLine();
            line = line.substring(line.indexOf(' ') + 1);
            //score sal coh
            queryInfo.setScore(Double.parseDouble(line.substring(0, line.indexOf(' '))));

            line = line.substring(line.indexOf(' ') + 1);
            //sal coh
            queryInfo.setSal(Double.parseDouble(line.substring(0, line.indexOf(' '))));

            line = line.substring(line.indexOf(' ') + 1);
            //coh
            queryInfo.setCoh(Double.parseDouble(line));
            ansList.add(queryInfo);
        }
        sc.close();
        return ansList;
    }

    public static void main(String[] args) throws IOException {
        AnsSummary as1 = new AnsSummary();
        as1.test();
    }

    /**
     * print the score between two algorithms
     *
     * @param ans1 compare
     * @param ans2 to compare
     */
    void compareScore(List<QueryInfo> ans1, List<QueryInfo> ans2) {
        int total = ans1.size();
        List<Integer> ans1Lose = new ArrayList<>();
        List<Integer> ans1Win = new ArrayList<>();
        double gd = 0D;
        double sd = 0D;
        for (int i = 0; i < total; i++) {
            if (ans1.get(i).getScore() - ans2.get(i).getScore() > 10E-8) {
                ans1Lose.add(ans1.get(i).query.size());
                gd += ans1.get(i).getScore() - ans2.get(i).getScore();
                /*StringBuilder sb = new StringBuilder();
                for (String st : ans2.get(i).query){
                    sb.append(';');
                    sb.append(st);
                }
                System.out.println(sb.substring(1));
                System.out.println((ans1.get(i).getScore() - ans2.get(i).getScore())
                        /ans2.get(i).getScore());*/
            }
            if (ans2.get(i).getScore() - ans1.get(i).getScore() > 10E-8) {
                ans1Win.add(ans1.get(i).query.size());
                sd += ans2.get(i).getScore() - ans1.get(i).getScore();

                StringBuilder sb = new StringBuilder();
                for (String st : ans2.get(i).query) {
                    sb.append(';');
                    sb.append(st);
                }
                System.out.println(sb.substring(1));
                System.out.println((ans2.get(i).getScore() - ans1.get(i).getScore())
                        / ans1.get(i).getScore());
            }

        }
        System.out.println(ans1Lose);
        System.out.println(ans1Win);
        System.out.println(total + " " + ans1Lose.size() + " " + ans1Win.size());
        System.out.println(gd / ans1Lose.size() + " " + sd / ans1Win.size());
    }

    /**
     * print the time between two algorithms
     *
     * @param ans1 compare
     * @param ans2 to compare
     */
    void compareTime(List<QueryInfo> ans1, List<QueryInfo> ans2) {
        int total = ans1.size();
        List<Integer> ans1Lose = new ArrayList<>();
        List<Integer> ans1Win = new ArrayList<>();
        double gd = 0D;
        double sd = 0D;
        for (int i = 0; i < total; i++) {
            if (ans1.get(i).getTime() - ans2.get(i).getTime() > 10E-1 * ans2.get(i).getTime()) {
                ans1Lose.add(ans1.get(i).query.size());
            }
            if (ans2.get(i).getTime() - ans1.get(i).getTime() > 10E-1 * ans1.get(i).getTime()) {
                ans1Win.add(ans1.get(i).query.size());

                StringBuilder sb = new StringBuilder();
                for (String st : ans2.get(i).query) {
                    sb.append(';');
                    sb.append(st);
                }
                System.out.println(sb.substring(1));
                System.out.println((ans2.get(i).getTime() - ans1.get(i).getTime())
                        / ans1.get(i).getTime());
            }

        }
        System.out.println(ans1Lose);
        System.out.println(ans1Win);
        System.out.println(total + " " + ans1Lose.size() + " " + ans1Win.size());
    }

    void CompareByKeywordNum(Map<String, List<QueryInfo>> totalResult) {
        String goal = "OneStarPoly";
        List<QueryInfo> baseR = totalResult.get(goal);
        for (Map.Entry<String, List<QueryInfo>> entry : totalResult.entrySet()) {
            String alg = entry.getKey();
            List<QueryInfo> algR = entry.getValue();
            List<List<Double>> times = new ArrayList<>();
            List<List<Double>> scores = new ArrayList<>();
            for (int i = 0; i < algR.size(); i++) {
                QueryInfo qi = algR.get(i);
                QueryInfo baseqi = baseR.get(i);
                if (!qi.getQueries().equals(baseqi.getQueries())) {
                    System.out.println("not matched");
                }
                //time
                int queryNum = qi.getQuery().size();
                while (times.size() <= queryNum) {
                    times.add(new ArrayList<>());
                }
                times.get(queryNum).add(qi.getTime());
                //score
                while (scores.size() <= queryNum) {
                    scores.add(new ArrayList<>());
                }
                if (baseqi.getScore() < 1E-8) {
                    scores.get(queryNum).add(1D);
                } else {
                    scores.get(queryNum).add(qi.getScore() / baseqi.getScore());
                }
            }
            System.out.print(alg);
            for (int i = 0; i < times.size(); i++) {
                if (times.get(i).size() == 0) {
                    //System.out.print("\t\t");
                    continue;
                }
                double sum = 0;
                for (double s : times.get(i)) {
                    sum += s;
                }
                System.out.print("\t" + String.format("%.1f", (sum / times.get(i).size())));
                sum = 0;
                for (double s : scores.get(i)) {
                    sum += s;
                }
                System.out.print("\t" + String.format("%.3f", (sum / scores.get(i).size())));
            }
            System.out.println();
        }

    }

    /**
     * print the avg time and timeout ratio
     * @param alg
     * @param al
     * @param totalResult
     */
    void printTimeAndOut(List<String> alg, double al, Map<String, List<QueryInfo>> totalResult) {
        System.out.println(al);
        for (String st : alg) {
            if (al != 0.9 && (st.equals("DPBFBase"))) {
                continue;
            }
            double cnt = 0;
            double avg = 0D;
            for (QueryInfo qi : totalResult.get(st)) {
                if (qi.getTime() > Util.CUTIME) {
                    cnt++;
                    avg += Util.CUTIME;
                } else {
                    avg = avg + qi.getTime();
                    //System.out.println(qi.getQuery());
                    //System.out.println(qi.getTime());
                }
            }
            System.out.print(st);
            System.out.printf(" %.2f", (cnt / totalResult.get(st).size() * 100));
            System.out.printf(" %.2f", (avg / totalResult.get(st).size() / 1000));
            System.out.println();

            cnt = 0;
            for (QueryInfo qi : totalResult.get(st)) {
                if (qi.getScore() < 0) {
                    cnt++;
                }
            }
            //System.out.printf(" %.2f\n", (cnt / totalResult.get(st).size() * 100));
        }
    }

    /**
     * print sal and coh ratio subject to dpbf
     * @param al
     * @param totalResult
     */
    void printRatioAndOut(double al, Map<String, List<QueryInfo>> totalResult) {
        System.out.println(al);
        List<String> alg = new ArrayList<>();
        alg.add("0.3.B3F");
        alg.add("0.7.B3F");
        for (String st : alg) {
            double cntSal = 0, cntCoh = 0;
            double avgSal = 0D, avgCoh = 0;
            for (int i = 0; i < totalResult.get(st).size(); i++) {
                QueryInfo qi = totalResult.get(st).get(i);
                QueryInfo base = totalResult.get("0.5.DPBFBase").get(i);
                if (base.getSal() > 0) {
                    cntSal++;
                    avgSal += qi.getSal() / base.getSal();
                }
                if (base.getCoh() > 0) {
                    cntCoh++;
                    avgCoh += qi.getCoh() /base.getCoh();
                }
            }
            System.out.print(st);
            //System.out.printf(" %.2f", (avgSal / cntSal));
            System.out.printf(" %.2f", (avgCoh / cntCoh));
            System.out.println();
        }
    }


    /**
     * print app ratio subject to b3f
     * @param al
     * @param totalResult
     */
    void printAppRatio(double al, Map<String, List<QueryInfo>> totalResult) {
        System.out.println(al);
        List<String> alg = new ArrayList<>();
        alg.add("DPBFBase");
        for (String st : alg) {
            double cnt = 0;
            double avg = 0D;
            for (int i = 0; i < totalResult.get(st).size(); i++) {
                QueryInfo qi = totalResult.get(st).get(i);
                QueryInfo base = totalResult.get("b3f").get(i);
                if (base.getScore() > 0) {
                    cnt++;
                    avg += qi.getScore() / base.getScore();
                }
            }
            System.out.print(st);
            System.out.printf(" %.2f", (avg / cnt));
            System.out.println();
        }
    }

    void test() throws IOException {
        String graph = Util.DBPEDIA20K;
        Util.setUSERDF2VEC(true);
        List<String> alg = new ArrayList<>();
        alg.add("b3f");
        alg.add("DPBFBase");
        alg.add("b3f");


        Map<String, List<QueryInfo>> totalResult = new TreeMap<>();
        double []als = new double[]{0.3, 0.5, 0.7};
        for (int i = 0; i < als.length;i ++) {
            Util.setAlpha(als[i]);
            String st = alg.get(i);
            totalResult.put(als[i] + "." + st, readAnsTxt(graph, st));
            //printTimeAndOut(alg, al, totalResult);
        }
        printRatioAndOut(0, totalResult);
        //compareTime(totalResult.get("BBAOld"), totalResult.get("BBA"));
        //CompareByKeywordNum(totalResult);
    }

    public static class QueryInfo {
        String queries;
        List<String> query;
        String alg;
        double time;
        double score;
        double sal;
        double coh;
        QueryInfo(String queries, List<String> query, String alg) {
            this.queries = queries;
            this.query = query;
            this.alg = alg;
        }

        public List<String> getQuery() {
            return query;
        }

        public String getQueries() {
            return queries;
        }

        public double getTime() {
            return time;
        }

        void setTime(double time) {
            this.time = time;
        }

        public double getScore() {
            return score;
        }

        void setScore(double score) {
            this.score = score;
        }

        public double getSal() {
            return sal;
        }

        public void setSal(double sal) {
            this.sal = sal;
        }

        public double getCoh() {
            return coh;
        }

        public void setCoh(double coh) {
            this.coh = coh;
        }
    }
}
