package b3f;

import graphdeal.ConnectedGraph;
import graphdeal.GraphBase;
import graphdeal.Util;

import java.util.Arrays;
import java.util.Properties;

/**
 * @Author Yuxuan Shi
 * @Date 11/6/2019
 * @Time 11:13 AM
 */
public class GraphB3F extends GraphBase {

    public int[] bitList;	//all entity have keyword bit to queryword
    private boolean bitFlag = false;

    private static GraphB3F instance = null;

    public static GraphB3F getInstance(){
        if (instance == null) {
            Properties pps = Util.getInitPPS();
            if (pps.get("INIT").equals("true")) {
                return new GraphB3F(pps.get("DATABASE").toString(),
                        pps.get("GRAPH_NAME").toString());
            }
        }
        return instance;
    }

    public static void closeInstance() {
        instance = null;
    }

    @Override
    protected void graphInit() {
        bitList = new int[biggestNodeNum];
        Arrays.fill(bitList, 0);
    }

    public GraphB3F(String database, String graphName){ readGraph(database, graphName); }
    public GraphB3F(String fileOrDir, String typeFileOrDir, String graphName){
        readGraph(fileOrDir, typeFileOrDir, graphName);
    }

    void bitClear(ConnectedGraph c1){
        bitFlag = false;
        if (keywordList == null) {
            return;
        }
        for (String it : keywordList) {
            for (Integer keyNode : c1.getKeynode(it)) {
                bitList[keyNode] = 0;
            }
        }
    }

    void bitSet(ConnectedGraph c1){
        if (bitFlag) {
            bitClear(c1);
        }
        bitFlag = true;

        for (int i = 0; i < queryKeyNum; i++) {
            for (Integer keyNode : c1.getKeynode(keywordList.get(i))) {
                bitList[keyNode] |= (1<<i);
            }
        }

    }
}
