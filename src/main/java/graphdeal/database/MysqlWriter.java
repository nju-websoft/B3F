package graphdeal.database;

import graphdeal.Util;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @Author Yuxuan Shi
 * @Date 11/25/2019
 * @Time 11:59 PM
 */
public class MysqlWriter implements Closeable {
    private String DBName;
    private String tableName;

    private Connection conn = null;
    private java.sql.Statement stmt = null;
    private PreparedStatement pstm = null;
    private String sql;
    private int count;
    private ModeEnum mode;
    private static int breakNum = 200000;

    private void modeChange(String table) {
        try {
            if (table.equals("nodeID") || table.equals("nodeIDPLL")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(subject VARCHAR(1023),"
                        + " newName INTEGER, weight DOUBLE)";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(subject, newName, weight) VALUES (?,?,?)";
                mode = ModeEnum.NodeID;
                return;
            }

            if (table.equals("hubLabel") || table.equals("hubLabelPLL")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(u INTEGER,"
                        + " v INTEGER, dis DOUBLE, par INTEGER)";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(u, v, dis, par) VALUES (?,?,?,?)";
                mode = ModeEnum.HubLabel;
                return;
            }

            if (table.equals("edgeWeight")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(weight DOUBLE)";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(weight) VALUES (?)";
                mode = ModeEnum.EdgeWeight;
                return;
            }

            if (table.equals("graphTriple")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(subject VARCHAR(1023),"
                        + "predicate VARCHAR(1023),"
                        + " object VARCHAR(1023))";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(subject, predicate, object) VALUES (?,?,?)";
                mode = ModeEnum.GraphTriple;
                return;
            }

            if (table.equals("pageRank")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(subject VARCHAR(1023),"
                        + " pg DOUBLE)";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(subject, pg) VALUES (?,?)";
                mode = ModeEnum.PageRank;
                return;
            }

            if (table.equals("typeTriple")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(subject VARCHAR(1023),"
                        + " predicate VARCHAR(1023),"
                        + " object VARCHAR(1023))";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(subject, predicate, object) VALUES (?,?,?)";
                mode = ModeEnum.TypeTriple;
                return;
            }

            //map type to short
            if (table.equals("typeID")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(subject VARCHAR(1023),"
                        + " typeId SMALLINT)";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(subject, typeId) VALUES (?,?)";
                mode = ModeEnum.TypeID;
                return;
            }

            //generated from 'typetriple', u is from 'nodeID', typeId is from 'typeID'
            if (table.equals("type")) {
                sql = "CREATE TABLE IF NOT EXISTS "+table+"(u INTEGER,"
                        + " typeId SMALLINT)";
                stmt.executeUpdate(sql);
                sql = "INSERT INTO "+table+"(u, typeId) VALUES (?,?)";
                mode = ModeEnum.Type;
                return;
            }

            if (table.equals("rdf2vec")) {
                StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS "+table+"(u INTEGER PRIMARY KEY");
                for (int i = 0; i < Util.RDFVEC; i++) {
                    sb.append(" ,d");
                    sb.append(i);
                    sb.append(" DOUBLE");
                }
                sb.append(")");
                sql = sb.toString();
                stmt.executeUpdate(sql);

                sb = new StringBuilder("INSERT INTO "+table+"(u");
                for (int i = 0; i < Util.RDFVEC; i++) {
                    sb.append(", d");
                    sb.append(i);
                }
                sb.append(") VALUES (?");
                for (int i = 0; i < Util.RDFVEC; i++) {
                    sb.append(", ?");
                }
                sb.append(")");
                sql = sb.toString();
                mode = ModeEnum.Rdf2vec;
                return;
            }
            System.out.println("Can't find table mode " + table);
            tableName = null;
        }catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public MysqlWriter() throws SQLException {
        conn = KCYDBCPUtil.getConnection();
        stmt = conn.createStatement();
    }

    public MysqlWriter(String database) throws SQLException {
        this();
        DBName = database;
        mode = ModeEnum.Undefined;
        sql = "CREATE DATABASE IF NOT EXISTS " + database;
        stmt.executeUpdate(sql);
        // use graphdeal.database
        sql = "USE " + database;
        stmt.executeUpdate(sql);
    }

    //using another datebase
    public void dbInit(String database, String table) throws SQLException {
        DBName = database;
        tableName = table;
        count = 0;
        if (mode != ModeEnum.Undefined) {
            writeEnd();  //clear last writer before writing to another table
        }
        sql = "CREATE DATABASE IF NOT EXISTS " + database;
        stmt.executeUpdate(sql);
        // use graphdeal.database
        sql = "USE " + database;
        stmt.executeUpdate(sql);

        sql = "DROP TABLE IF EXISTS "+ table;
        stmt.executeUpdate(sql);

        modeChange(table);

        pstm = conn.prepareStatement(sql);
    }

    //using the same graphdeal.database
    public void dbInit(String table) throws SQLException {
        if (DBName == null){
            System.out.println("Need to assign a graphdeal.database first!");
            return;
        }
        if (mode != ModeEnum.Undefined) {
            writeEnd();  //clear last writer before writing to another table
        }
        tableName = table;
        count = 0;
        sql = "DROP TABLE IF EXISTS "+ table;
        stmt.executeUpdate(sql);
        modeChange(table);
        pstm = conn.prepareStatement(sql);
    }


    //inset subName into graphdeal.database
    public void insertNodeID(String subject, int newName, double weight) throws SQLException {
        if (mode != ModeEnum.NodeID) {
            System.out.println("table is not " + tableName);
            return;
        }
        count++;
        pstm.setString(1, subject);
        pstm.setInt(2, newName);
        pstm.setDouble(3, weight);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    //inset hubLabel into graphdeal.database
    public void insertHubLabel(int u, int v, double dis, int par) throws SQLException {
        if (mode != ModeEnum.HubLabel){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        count++;
        pstm.setInt(1, u);
        pstm.setInt(2, v);
        pstm.setDouble(3, dis);
        pstm.setInt(4, par);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    //inset edgeWeight into graphdeal.database
    public void insertEdgeWeight(double weight) throws SQLException {
        if (mode != ModeEnum.EdgeWeight){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        count++;
        pstm.setDouble(1, weight);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    //inset graph into graphdeal.database
    public void insertGraphTriple(String x, String y, String z) throws SQLException {
        if (mode != ModeEnum.GraphTriple){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        count++;
        pstm.setString(1, x);
        pstm.setString(2, y);
        pstm.setString(3, z);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    //insert pagerank into graphdeal.database
    public void insertPageRank(String x, double weight) throws SQLException {
        if (mode != ModeEnum.PageRank){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        count++;
        pstm.setString(1, x);
        pstm.setDouble(2, weight);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    public void insertTypeTriple(String x, String y, String z) throws SQLException {
        if (mode != ModeEnum.TypeTriple){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        count++;
        pstm.setString(1, x);
        pstm.setString(2, y);
        pstm.setString(3, z);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    public void insertTypeID(String x, short y) throws SQLException {
        if (mode != ModeEnum.TypeID){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        count++;
        pstm.setString(1, x);
        pstm.setShort(2, y);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    public void insertType(int x, short y) throws SQLException {
        if (mode != ModeEnum.Type){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        count++;
        pstm.setInt(1, x);
        pstm.setShort(2, y);
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    public void insertRdf2Vec(int x, List<Double> y) throws SQLException {
        if (mode != ModeEnum.Rdf2vec){
            System.out.println("Fault! table is not " + tableName);
            return;
        }
        assert y.size() == Util.RDFVEC;
        count++;
        pstm.setInt(1, x);
        for (int i = 0; i < Util.RDFVEC; i++) {
            pstm.setDouble(i + 2, y.get(i));
        }
        pstm.addBatch();
        if (count % breakNum == 0) {
            System.out.println(count);
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
        }
    }

    //write all
    public void writeEnd() throws SQLException {
        if (pstm != null && !pstm.isClosed()) {
            pstm.executeBatch();
            conn.commit();
            pstm.clearBatch();
            pstm.close();
        }
        if (mode == ModeEnum.HubLabel && tableName.equals("hubLabel")) {
            stmt.executeUpdate("CREATE INDEX uv ON "+ tableName +" (u,v)");
        }
        mode = ModeEnum.Undefined;
        pstm = null;
        tableName = null;
    }

    //close all link
    @Override
    public void close(){
        try {
            writeEnd();
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            mode = ModeEnum.Undefined;
        }
    }
}
