package dpbf;

import graphdeal.GraphBase;
import graphdeal.Util;

import java.util.Properties;

public class GraphDPBF extends GraphBase {
	private static GraphDPBF instance = null;
	public static GraphDPBF getInstance(){
		if (instance == null) {
			Properties pps = Util.getInitPPS();
			if (!"false".equals(pps.get("INIT"))) {
				instance = new GraphDPBF(pps.get("DATABASE").toString(),
						pps.get("GRAPH_NAME").toString());
			}
		}
		return instance;
	}

	public static void closeInstance() {
		instance = null;
	}

	@Override
	protected void graphInit(){
	}

	public GraphDPBF(String database, String graphName){
		readGraph(database, graphName);
	}

	public GraphDPBF(String fileOrDir, String typeFileOrDir, String graphName){
		readGraph(fileOrDir, typeFileOrDir, graphName);
	}
}
