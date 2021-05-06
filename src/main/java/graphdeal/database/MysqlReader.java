package graphdeal.database;

import graphdeal.Util;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author Yuxuan Shi
 * @Date 11/26/2019
 * @Time 5:32 PM
 */
public class MysqlReader implements Closeable {

    String DBName;
    String tableName;

    Connection conn = null;
    java.sql.Statement stmt = null;
    ResultSet rs = null;
    ModeEnum mode = ModeEnum.Undefined;

    public MysqlReader() throws SQLException {
        conn = KCYDBCPUtil.getConnection();
        stmt = conn.createStatement();
    }

    public MysqlReader(String database) throws SQLException {
        this();
        DBName = database;
        String sql;
        stmt = conn.createStatement();
        sql = "CREATE DATABASE IF NOT EXISTS " + database;
        stmt.executeUpdate(sql);
        // use graphdeal.database
        sql = "USE " + database;
        stmt.executeUpdate(sql);
    }

    void modeChange(String table) {
        if (table.equals("nodeID")){
            mode = ModeEnum.NodeID;
            return;
        }
        if (table.equals("graphTriple")){
            mode = ModeEnum.GraphTriple;
            return;
        }
        if (table.equals("pageRank")){
            mode = ModeEnum.PageRank;
            return;
        }
        if (table.equals("type")){
            mode = ModeEnum.Type;
            return;
        }
        if (table.equals("typeTriple")){
            mode = ModeEnum.TypeTriple;
            return;
        }
        if (table.equals("typeID")){
            mode = ModeEnum.TypeID;
            return;
        }
        if (table.equals("rdf2vec")){
            mode = ModeEnum.Rdf2vec;
            return;
        }
        System.out.println("No such table " + table+ " found!");
        tableName = null;
    }

    public void dbInit(String database, String table) throws SQLException {
        close();
        DBName = database;
        tableName = table;
        String sql;
        stmt = conn.createStatement();
        sql = "CREATE DATABASE IF NOT EXISTS " + database;
        stmt.executeUpdate(sql);
        // use graphdeal.database
        sql = "USE " + database;
        stmt.executeUpdate(sql);

        sql = "SELECT * from " + table;
        stmt.setFetchSize(100000);
        rs = stmt.executeQuery(sql);
        /*if (rs.wasNull())
            System.out.println("Open table " + table + " fail");*/
        modeChange(table);
    }

    public void dbInit(String table) throws SQLException {
        tableName = table;
        String sql;
        sql = "SELECT * from " + table;
        stmt.setFetchSize(100000);
        rs = stmt.executeQuery(sql);
        /*if (rs.wasNull())
            System.out.println("Open table " + table + " fail");*/
        modeChange(table);
    }

    @Deprecated
    public boolean readGraphTriple(String[] triples) throws SQLException {
        if (mode != ModeEnum.GraphTriple){
            System.out.println("Fault! table is not " + tableName);
            return false;
        }
        if (rs.isClosed())
            return false;
        if (rs.next()) { // get a new line
            triples[0] = rs.getString("subject");
            triples[1] = rs.getString("predicate");
            triples[2] = rs.getString("object");
            return true;
        } else {
            System.out.println("GraphTriple read end!");
            rs.close();
            return false;
        }
    }

    /**
     * get a triple
     * @return 0:entity(String), 1:relation(String), 2:entity(String)
     * @throws SQLException
     */
    public List<Object> readGraphTriple() throws SQLException {
        if (mode != ModeEnum.GraphTriple){
            System.out.println("Fault! table is not " + tableName);
            return null;
        }
        if (rs.isClosed()) {
            return null;
        }
        if (rs.next()) {
            List<Object> ans = new ArrayList<>();
            ans.add(rs.getString("subject"));
            ans.add(rs.getString("predicate"));
            ans.add(rs.getString("object"));
            return ans;
        } else {
            System.out.println("GraphTriple read end!");
            rs.close();
            return null;
        }
    }
    /**
     * get node's name, its id and its weight
     * @return 0:node(string), 1:node id(Integer), 2:node weight(Double)
     * @throws SQLException
     */
    public List<Object> readNodeID() throws SQLException {
        if (mode != ModeEnum.NodeID){
            System.out.println("Fault! table is not " + tableName);
            return null;
        }
        if (rs.isClosed()) {
            return null;
        }
        if (rs.next()) {
            List<Object> ans = new ArrayList<>();
            ans.add(rs.getString("subject"));
            ans.add(rs.getInt("newName"));
            ans.add(rs.getDouble("weight"));
            return ans;
        } else {
            System.out.println("NodeID read end!");
            rs.close();
            return null;
        }
    }

    @Deprecated
    public boolean readNodeID(String[] singleK, int[] singleN) throws SQLException {
        if (mode != ModeEnum.NodeID){
            System.out.println("Fault! table is not " + tableName);
            return false;
        }
        if (rs.isClosed()) {
            return false;
        }
        if (rs.next()) { // get a new line
            singleK[0] = rs.getString("subject");
            singleN[0] = rs.getInt("newName");
            return true;
        } else {
            System.out.println("NodeID read end!");
            rs.close();
            return false;
        }
    }

    public boolean readTypeTriple(String[] triples) throws SQLException {
        if (mode != ModeEnum.TypeTriple){
            System.out.println("Fault! table is not " + tableName);
            return false;
        }
        if (rs.isClosed())
            return false;
        if (rs.next()) { // get a new line
            triples[0] = rs.getString("subject");
            triples[1] = rs.getString("predicate");
            triples[2] = rs.getString("object");
            return true;
        } else {
            System.out.println("TypeTriple read end!");
            rs.close();
            return false;
        }
    }

    @Deprecated
    public boolean readTypeID(String[] singleK, short[] singleS) throws SQLException {
        if (mode != ModeEnum.TypeID){
            System.out.println("Fault! table is not " + tableName);
            return false;
        }
        if (rs.isClosed())
            return false;
        if (rs.next()) {
            singleK[0] = rs.getString("subject");
            singleS[0] = rs.getShort("typeId");
            return true;
        } else {
            System.out.println("TypeID read end!");
            rs.close();
            return false;
        }
    }

    /**
     * get a type and its id
     * @return 0:type name(String), 1: type id(Short)
     * @throws SQLException
     */
    public List<Object> readTypeID() throws SQLException {
        if (mode != ModeEnum.TypeID){
            System.out.println("Fault! table is not " + tableName);
            return null;
        }
        if (rs.isClosed()) {
            return null;
        }
        if (rs.next()) {
            List<Object> ans = new ArrayList<>();
            ans.add(rs.getString("subject"));
            ans.add(rs.getShort("typeId"));
            return ans;
        } else {
            System.out.println("TypeID read end!");
            rs.close();
            return null;
        }
    }

    @Deprecated
    public boolean readType(int[] singleI, short[] singleS) throws SQLException {
        if (mode != ModeEnum.Type){
            System.out.println("Fault! table is not " + tableName);
            return false;
        }
        if (rs.isClosed()) {
            return false;
        }
        if (rs.next()) {
            singleI[0] = rs.getInt("u");
            singleS[0] = rs.getShort("typeId");
            return true;
        } else {
            System.out.println("Type read end!");
            rs.close();
            return false;
        }
    }

    /**
     * get a node u and it's type
     * @return 0:node(Integer), 1:type id(Integer)
     * @throws SQLException
     */
    public List<Object> readType() throws SQLException {
        if (mode != ModeEnum.Type){
            System.out.println("Fault! table is not " + tableName);
            return null;
        }
        if (rs.isClosed()) {
            return null;
        }
        if (rs.next()) {
            List<Object> ans = new ArrayList<>();
            ans.add(rs.getInt("u"));
            ans.add(rs.getShort("typeId"));
            return ans;
        } else {
            System.out.println("Type read end!");
            rs.close();
            return null;
        }
    }

    public List<Object> readRdf2Vec() throws SQLException {
        if (mode != ModeEnum.Rdf2vec){
            System.out.println("Fault! table is not " + tableName);
            return null;
        }
        if (rs.isClosed()) {
            return null;
        }
        if (rs.next()) {
            List<Object> ans = new ArrayList<>();
            ans.add(rs.getInt("u"));
            for (int i = 0; i < Util.RDFVEC; i++) {
                ans.add(rs.getDouble("d" + i));
            }
            return ans;
        } else {
            System.out.println("Rdf2Vec read end!");
            rs.close();
            return null;
        }
    }

    @Override
    public void close(){
        try {
            if (!rs.isClosed()) {
                rs.close();
            }
            if (!stmt.isClosed()) {
                stmt.close();
            }
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            mode = ModeEnum.Undefined;
        }
    }
}
