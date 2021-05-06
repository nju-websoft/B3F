This is the java implementation of WWW paper "Efficient Computation of Semantically Cohesive Subgraphs for Keyword-Based Knowledge Graph Exploration". It is based on jdk11.

* Directory /src/main/java/com/bosch contains all the source code.

	'qgstp' is the implementation of the proposed algorithms.

	'banksII' is the implementation of BANKS-II

	'database' is the reader and writer class of database

	'dpbf' is the implementation of DPBF

	'graphdeal' contains common classes used by other classes. It also includes 
the writer of DBPEDIA, subgraph of DBPEDIA and LUBM.

	'queryGen' converts origin query to query that matches current graph.

* Directory /src/main/resources contains all the queries, results reported in 
the experiments , database settings and graph to be tested.

* Directory /test contains the code to run the experiments.
	
	'TestAllGivenNode.java' is the code to run on dbpedia.
	
	'TestAllLubmNode.java' is the code to run on lubm.
