package com.szgx.kunpeng;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.sql.DriverManager;

@SpringBootTest
class KunpengApplicationTests {

    @Test
    void contextLoads() {
    }


    @Test
    public void getConnect()
    {
        //驱动类。
        String driver = "org.openGauss.Driver";
        //数据库连接描述符。
        String sourceURL = "jdbc:postgresql://139.159.187.78:15400/mydb";
        String username="coder";
        String passwd = "qwE_23sz123";
        Connection conn = null;

        try
        {
            //加载驱动。
            Class.forName(driver);
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            //创建连接。
            conn = DriverManager.getConnection(sourceURL, username, passwd);
            System.out.println("Connection succeed!");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    };
}
