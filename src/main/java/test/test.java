package test;


import com.alibaba.fastjson.JSONArray;
import framework.session.SqlSessionFactory;

public class test
{
    public static void main (String[] args) throws Exception
    {
//        Student stu=new Student();
//        stu.setName("jkl");
//        SqlSessionFactory.insert(stu);
        String sql="select * from student";
        JSONArray array=SqlSessionFactory.executeQuery2JSONArray(sql);
        System.out.println(array);

    }
}


