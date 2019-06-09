package db.mapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
 * 表示类与表的映射关系
 * */
public class Configuration
{
    public String name;  //类名
    public String table;  //表名
    public Class clazz;

    //存放属性
    public List<Property> properties = new ArrayList<>();
    public String generatedKey; // 自增主键的列名

    public Map<String, Method> getters = new HashMap<>();
    public Map<String, Method> setters = new HashMap<>();



    public static class Property
    {
        public String name;// 属性名, 须与列名相同
        public String type; // 映射成的Java类
        public boolean primaryKey = false;// 未使用
        public int displaySize = 0; // 未使用
        public boolean isNotNull = false; // 未使用
    }
}
