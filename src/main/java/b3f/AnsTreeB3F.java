package b3f;

import graphdeal.AnsTree;
import graphdeal.ConnectedGraph;
import graphdeal.Util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Yuxuan Shi
 * @Date 12/11/2019
 * @Time 9:33 AM
 */
public class AnsTreeB3F extends AnsTree {

    class HashUnionFind {
        Map<Integer, Integer> par;
        HashUnionFind(Collection<Integer> list) {
            par = new HashMap<>();
            for (int u : list) {
                par.put(u, u);
            }
        }

        int find(int u) {
            if (par.get(u) != u) {
                par.put(u, find(par.get(u)));
            }
            return par.get(u);
        }

        void union(int u, int v) {
            par.put(find(u), find(v));
        }
    }
    AnsTreeB3F(ConnectedGraph c1, int r){
        super(c1, r);
    }

    AnsTreeB3F(ConnectedGraph c1, int r, List<List<Map.Entry<Integer, Integer>>> mergePaths){
        super(c1, r);
        for (List<Map.Entry<Integer, Integer>> list : mergePaths) {
            addPathVertice(list);
        }
        HashUnionFind huf = new HashUnionFind(nodes);
        for (List<Map.Entry<Integer, Integer>> list : mergePaths) {
            for (Map.Entry<Integer, Integer> it : list) {
                int u = it.getKey();
                int v = it.getValue();
                if (huf.find(u) != huf.find(v)) {
                    huf.union(u, v);
                    edges.add(it);
                }
            }
        }
        assert nodes.size() == edges.size() + 1;
        calcScore(Util.ALPHA);
    }

    //add path from root to leaf
    public void addPathVertice(List<Map.Entry<Integer, Integer>> paths) {
        for (int i = paths.size() - 1; i >= 0; i--){
            Map.Entry<Integer, Integer> it = paths.get(i);
            int u = it.getKey();
            int v = it.getValue();
            nodes.add(u);
            nodes.add(v);
        }
    }
}
