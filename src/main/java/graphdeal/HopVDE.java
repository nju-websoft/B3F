package graphdeal;

/**
 * @Author Yuxuan Shi
 * @Date 10/31/2019
 * @Time 9:11 AM
 */
public class HopVDE{
    public int v;
    private double dis;
    public int edgeID;

    //basic distance
    public double getDis(){
        return dis;
    }

    //dynamic distance
    public double getDis(ConnectedGraph c1, int u){
        return dis + c1.getNodeWeight(u) + c1.getNodeWeight(v);
    }
    public HopVDE(int v, double dis, int edgeID){
        this.v = v;
        this.dis = dis;
        this.edgeID = edgeID;
    }

    public HopVDE(int v, int edgeID){
        this.v = v;
        this.dis = 0D;
        this.edgeID = edgeID;
    }
}
