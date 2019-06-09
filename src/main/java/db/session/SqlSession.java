package db.session;

import db.mapping.ConfigurationManager;
import db.mapping.Configuration;
import db.sql.SqlInsert;
import db.utils.ReflectUtil;

import java.lang.reflect.Method;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


/*
 * 将从c3p0连接池中获取的连接进行封裝
 * 提供事務
 * 提供基础的增删改查的接口
 * */
public class SqlSession
{
    public Connection connection;
    public String lastSQL = "";

    public SqlSession ()
    {
    }

    public SqlSession (Connection connection)
    {
        this.connection = connection;
    }

    //连接数据库
    public void connect (String ip, int port, String catalog, String username, String password) throws Exception
    {
        String fmt = "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8";
        String connectionURL = String.format(fmt, ip, port, catalog);
        connect(connectionURL, username, password);
    }

    public void connect (String connectionUrl, String username, String password) throws Exception
    {
        connection = DriverManager.getConnection(connectionUrl, username, password);
    }

    //关闭连接
    public void close () throws Exception
    {
        connection.close();
    }

    // 最近一次query的sql
    public String getLastSQL ()
    {
        return lastSQL;
    }

    // 事务
    public void beginTransaction () throws SQLException
    {
        connection.setAutoCommit(false);
    }

    public void commitTransaction () throws SQLException
    {
        connection.commit();
    }

    public void rollbackTransaction () throws SQLException
    {
        connection.rollback();
    }

    //删，改
    public void execute (String sql) throws Exception
    {
        this.lastSQL = sql;
        Statement statement = connection.createStatement();
        statement.execute(sql);
    }

    //执行查询，返回结果集
    public ResultSet executeQuery (String sql) throws Exception
    {
        this.lastSQL = sql;
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        return rs;
    }

    // 执行查询, 并自动映射 (不要求POJO中有注解)
    public<T> List<T> executeQuery (String sql, Class<T> clazz) throws Exception
    {
        this.lastSQL = sql;
        Statement stmt = connection.createStatement();
        ResultSet resultSet = stmt.executeQuery(sql);

        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnsCount = resultSetMetaData.getColumnCount(); //一共有几列
        String[] labels = new String[columnsCount]; // 每列的标题
        int[] types = new int[columnsCount];  // 每列的类型
        for (int i = 0; i < columnsCount; i++)
        {
            int idx = i + 1;
            labels[i] = resultSetMetaData.getColumnLabel(idx);
            types[i] = resultSetMetaData.getColumnType(idx);
        }


        Configuration sqlPojo = ConfigurationManager.i.findClassByClassName(clazz.getName());
        if(sqlPojo == null) //从注解提取信息
            sqlPojo = ConfigurationManager.i.findClassByAnnotation(clazz);


        List<T> result = new ArrayList<>();

        while (resultSet.next())
        {
            T pojo = clazz.newInstance();
            result.add(pojo);
            for (int i = 0; i < columnsCount; i++)
            {
                int idx = i + 1;
                String columnValue = resultSet.getString(idx); // 每列的值
                Method method = sqlPojo.setters.get(labels[i]);

                try
                {
                    ReflectUtil.map(pojo, method, types[i], columnValue);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


//  核心就是拼湊出SQL語句
    public void insert (Object pojo) throws Exception
    {
        Class clazz = pojo.getClass();

        Configuration pojoConfig = ConfigurationManager.i.findClassByClassName(clazz.getName());
        if(pojoConfig == null)
            pojoConfig = ConfigurationManager.i.findClassByAnnotation(pojo.getClass()); // 从注解来提取

        if(pojoConfig.table == null)
            throw new Exception("类 " + clazz.getName() + "中无AFTABLE注解! 无法自动插入!");


        SqlInsert si = new SqlInsert(pojoConfig.table);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        for (Configuration.Property p : pojoConfig.properties) // sc.properties
        {
            String fieldName = p.name;

//            Method getter = ReflectUtil.findGetter(clazz, fieldName);
            Method getter = pojoConfig.getters.get(fieldName);
            try
            {
                Object value = getter.invoke(pojo);
                if(value != null)
                {
                    if(value instanceof Boolean)
                    {
                        Boolean v = (Boolean) value;
                        value = v ? "1" : "0";
                    } else if(value instanceof Date)
                    {
                        Date v = (Date) value;
                        value = sdf.format(v);
                    }

                    si.add(fieldName, value.toString());
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        String sql = si.toString();
        this.lastSQL = sql;

        Statement stmt = connection.createStatement();
        if(pojoConfig.generatedKey == null)
        {
            // 无自增ID
            stmt.execute(sql);
        } else
        {
            // 自增ID处理
            // 1 如果用户在插入时已经自己指定了一个值，则MySQL会使用这个值，并返回这个值
            // 2 如果用户在插入时未定自增主键的值，则MySQL会生成一个自增的值，并返回
            stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet keys = stmt.getGeneratedKeys();
            if(keys.next())
            {
                // 取回自增的ID
                String id = keys.getString(1);
                try
                {
                    Method setter = ReflectUtil.findSetter(clazz, pojoConfig.generatedKey);
                    ReflectUtil.setIntValue(pojo, setter, id);
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
