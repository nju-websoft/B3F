package experiment;

import graphdeal.AnsTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author Yuxuan Shi
 * @Date 2021/1/14
 * @Time 14:17
 **/
public class QueryInfo {
    List<String> query;
    String alg;
    AnsTree ans;
    double midTime;
    List<Double> timeList;

    public QueryInfo(List<String> query, String alg) {
        this.query = query;
        this.alg = alg;
        timeList = new ArrayList<>();
        ans = null;
    }

    public  double getFirstTime() {
        if (timeList.size() == 0) {
            return 0D;
        }
        return timeList.get(0);
    }

    public void addTime(double time) {
        timeList.add(time);
    }

    public double getTime() {
        return midTime;
    }

    public void setAns(AnsTree ans) {
        this.ans = ans;
    }

    public AnsTree getAns() {
        return ans;
    }

    public void calcMid() {
        Collections.sort(timeList);
        if (timeList.size() == 0) {
            midTime = 0D;
            return;
        }
        if ((timeList.size() % 2) == 1) {
            midTime = timeList.get(timeList.size() / 2);
            return;
        }
        //now timelist has even number
        midTime = (timeList.get(timeList.size() / 2) + timeList.get(timeList.size() / 2 - 1)) / 2;
    }

    public double getScore() {
        if (ans == null)
            return -1D;
        return ans.getScore();
    }

    public double getSal() {
        if (ans == null)
            return -1D;
        return ans.getSal();
    }

    public double getCoh() {
        if (ans == null)
            return -1D;
        return ans.getCoh();
    }

    public String queryString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < query.size() - 1; i++) {
            sb.append(query.get(i));
            sb.append(";");
        }
        sb.append(query.get(query.size() - 1));
        return sb.toString();
    }
}
