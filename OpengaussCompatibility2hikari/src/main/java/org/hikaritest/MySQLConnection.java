package org.hikaritest;

import java.sql.*;

public class MySQLConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://192.168.131.66:3308/proto_test_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "proto_test";
        String password = "Proto_test123";

        try {
            // 加载 MySQL JDBC 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC 驱动已加载。");

            // 获取数据库连接
            Connection connection = DriverManager.getConnection(url, username, password);


            if (connection != null) {
                System.out.println("成功连接到数据库！");
                System.out.println(connection.toString());

                // 使用 PreparedStatement 执行 SQL 查询
                String query = "SET SESSION transaction isolation LEVEL REPEATABLE READ;";
                try (PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                    try (ResultSet rs = stmt.executeQuery()) {
//                        if (rs.next()) {
//                            System.out.println("查询成功，结果不为空。");
//                            String result = rs.getString(1); // 获取查询的第一个结果
//                            System.out.println("查询结果: " + result);
//                        } else {
//                            System.out.println("结果集为空。");
//                        }
                    }
                }
                query = "SELECT @@session.transaction_isolation";
                try (PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("查询成功，结果不为空。");
                            String result = rs.getString(1); // 获取查询的第一个结果
                            System.out.println("查询结果: " + result);
                        } else {
                            System.out.println("结果集为空。");
                        }
                    }
                }

                // 关闭连接
                connection.close();
            }


        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            // 设置 fetch size
            PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM test");
            pstmt.setFetchSize(1000);  // 这里使用游标来逐步获取数据

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // 处理每一行数据
                System.out.println("Data: " + rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
