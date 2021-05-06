package graphdeal.database;

import graphdeal.Util;
import org.apache.commons.dbcp2.BasicDataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * graphdeal.database pool usage
 *
 * @Author Yuxuan Shi
 * @date 2020/5/8
 */
public class KCYDBCPUtil {
    private static DataSource dataSource;
    //加载DBCP配置文件
    static{
        try{
            Properties pps = Util.getProperties("dbcp.properties");
            dataSource = BasicDataSourceFactory.createDataSource(pps);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //从连接池中获取一个连接
    public static Connection getConnection(){
        Connection connection = null;
        try{
            connection = dataSource.getConnection();
            assert connection != null;
            connection.setAutoCommit(false);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return connection;
    }
}