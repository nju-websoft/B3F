package dpbf;

import graphdeal.AnsTree;
import graphdeal.ConnectedGraph;
import graphdeal.Util;

import java.util.AbstractMap;

/**
 * @Author Yuxuan Shi
 * @Date 11/11/2019
 * @Time 12:49 PM
 */
public class AnsTreeDPBFNode extends AnsTree{

    AnsTreeDPBFNode(ConnectedGraph c1, CandiTree ct){
        super(c1, ct.v);
        for (EdgeDPBF it : ct.edges){
            nodes.add(it.u);
            nodes.add(it.v);
            edges.add(new AbstractMap.SimpleEntry<>(it.u, it.v));
        }
        //weight = ct.weight;
        calcScore(Util.ALPHA);
    }

   // public double getWeight(){ return weight;}
}
