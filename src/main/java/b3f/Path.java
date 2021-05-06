package b3f;

import graphdeal.Util;

/**
 * @Author Yuxuan Shi
 * @Date 11/6/2019
 * @Time 11:02 AM
 */
public class Path implements Comparable<Path>{
    double sigmaWT;
    double sigmaSD;
    double score;
    Path former = null;
    int node;
    short length = 0;
    int bitM;

    Path(int bit, int v){
        node = v;
        bitM = bit;
        sigmaWT = 0;
        sigmaSD = 0;
        score = 0;
        length = 1;
    }

    Path(Path p1, int v){
        this.node = v;
        this.former = p1;
        this.bitM = p1.bitM;
        this.sigmaSD = p1.sigmaSD;
        this.sigmaWT = p1.sigmaWT;
        this.length = (short) (p1.length + 1);
    }

    void WTAdd(double t) {
        sigmaWT= sigmaWT + t;
    }

    public double getWeight() {
        return Util.ALPHA * sigmaWT + Util.BETA * sigmaSD;
    }
    @Override
    public int compareTo(Path c2) {
        return score > c2.score? 1 : -1;
    }
}
