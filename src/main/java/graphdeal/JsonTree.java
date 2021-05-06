package graphdeal;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Author Yuxuan Shi
 * @Date 12/9/2019
 * @Time 5:16 PM
 */
public class JsonTree {
    public String title;
    public String graphJson;
    public double score;

    public JsonTree(){
        title = null;
        graphJson = null;
    }

    public static String formatEntity(String entity){
        int loc = entity.lastIndexOf('/');
        if (loc == entity.length() - 1)
            entity = entity.substring(0, loc);
        loc = entity.lastIndexOf('/');
        if (loc == -1) return "";   //blank node
        return entity.substring(loc + 1);
    }

    public static String formatlink(String entity){
        int loc = entity.lastIndexOf('/');
        if (loc == entity.length() - 1)
            entity = entity.substring(0, loc);
        loc = entity.lastIndexOf('/');
        if (loc == -1) return "";   //blank node
        entity = entity.substring(loc + 1);
        loc = entity.lastIndexOf('#');
        if (loc == -1) return entity;
        return entity.substring(loc + 1);
    }
    public void buildTree(AnsTree ans, String alg){
        title = alg;
        score = ans.score;
        Map<String, List<Map>> jsTree = new TreeMap<>();
        Map<Integer, Integer> jsMap = new TreeMap<>();
        for (Integer it : ans.nodes) jsMap.put(it, jsMap.size());
        //add nodes
        jsTree.put("nodes",new ArrayList<>());
        for (Integer it : ans.nodes) {
            Map jsNode = new TreeMap();
            jsNode.put("name", formatEntity(ans.c1.getIDToNode(it)));
            jsNode.put("id", jsMap.get(it));
            jsNode.put("symbolSize", 70);
            jsTree.get("nodes").add(jsNode);
        }

        jsTree.put("links",new ArrayList<>());
        for (Map.Entry<Integer, Integer> it : ans.edges){
            Map jsEdge = new TreeMap();
            jsEdge.put("source", jsMap.get(it.getKey()));
            jsEdge.put("target", jsMap.get(it.getValue()));
            jsEdge.put("name", formatlink(
                    ans.c1.getIDToEdge(ans.c1.binarySearchID(it.getKey(), it.getValue()))));
            jsTree.get("links").add(jsEdge);
        }
        graphJson = JSON.toJSON(jsTree).toString();
        System.out.println(graphJson);
    }
}
