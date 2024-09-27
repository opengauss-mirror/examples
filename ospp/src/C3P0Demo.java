import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.io.File;
import java.sql.*;


public class C3P0Demo {

    // 打印 employees 表中的数据
    private static void printEmployees(Statement stmt) throws SQLException {
        java.sql.ResultSet rs = stmt.executeQuery("SELECT * FROM employees");
        System.out.println("Employees Table:");
        while (rs.next()) {
            System.out.println("ID: " + rs.getInt("id") + ", Name: " + rs.getString("name")
                    + ", Role: " + rs.getString("role") + ", Salary: " + rs.getDouble("salary"));
        }
    }

    public static void main(String[] args) {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass("com.mysql.cj.jdbc.Driver");
            cpds.setJdbcUrl("jdbc:mysql://localhost:3306/test_db?allowLoadLocalInfile=true");
            cpds.setUser("root");
            cpds.setPassword("123456");
            cpds.setInitialPoolSize(10);
            cpds.setMinPoolSize(10);
            cpds.setMaxPoolSize(50);
            cpds.setAcquireIncrement(5);
            cpds.setCheckoutTimeout(20000); // 增加超时时间
            cpds.setMaxIdleTime(60);

            //连接数据库
            System.out.println("Attempting to get a connection...");
            try (Connection conn = cpds.getConnection()) {
                System.out.println("Connection successful!");
            } catch (SQLException e) {
                System.err.println("Failed to get a connection.");
                e.printStackTrace();
            }


            // 使用不同的事务隔离级别分别启动事务A和事务B
            /*
            System.out.println("Running with READ COMMITTED isolation level:");
            runTransactions(cpds, Connection.TRANSACTION_READ_COMMITTED);
            System.out.println("\nRunning with REPEATABLE READ isolation level:");
            runTransactions(cpds, Connection.TRANSACTION_REPEATABLE_READ);
            System.out.println("\nRunning with SERIALIZABLE isolation level:");
            runTransactions(cpds, Connection.TRANSACTION_SERIALIZABLE);
             */

            //查询操作
            /*
            executeDML(cpds);
            executeComplexQuery(cpds);
            executeMultiQuery(cpds);
             */

            //DDL操作
            /*
            executeDDL(cpds);
            executeView(cpds);
             */

            //DCL操作
            /*
            userPermission(cpds);
            roleManagement(cpds);
             */

            //聚合函数和窗口函数
            /*
            aggregation(cpds);
            windowFun(cpds);
             */

            //存储过程
            /*
            createStored(cpds);
            callStored(cpds,6000.00);
             */

            //分区
            /*
            createPart(cpds);
            insertPart(cpds);
            queryPart(cpds);
             */

            //触发器
            /*
            createTrigger(cpds);
            insert(cpds);
            queryAuditLog(cpds);
             */

            //UDFs
            /*
            createFunctions(cpds);
            callScalar(cpds);
            callTable(cpds);
             */

            //CTE
            //CTEQuery(cpds);

            //json&xml
            /*
            insertJSON(cpds);
            queryJSON(cpds);
            insertXML(cpds);
            queryXML(cpds);
             */

            //批量数据操作
            importData(cpds);
            exportData(cpds);



        } catch (Exception e) {
            System.err.println("Failed to initialize the connection pool.");
            e.printStackTrace();
        }
    }
/**
 * 事务管理
 * **/
    private static void runTransactions(ComboPooledDataSource cpds, int isolationLevel) throws InterruptedException {
        // 创建两个线程模拟事务A和事务B
        Thread transactionA = new Thread(() -> transactionA(cpds, isolationLevel));
        Thread transactionB = new Thread(() -> transactionB(cpds, isolationLevel));

        // 启动两个事务并发操作
        transactionA.start();
        Thread.sleep(1000);  // 确保事务A先执行，事务B稍后执行
        transactionB.start();

        // 等待两个事务执行完毕
        transactionA.join();
        transactionB.join();
    }

    // 事务A：插入和更新操作（不提交）
    private static void transactionA(ComboPooledDataSource cpds, int isolationLevel) {
        try (Connection conn = cpds.getConnection()) {
            // 设置事务隔离级别
            conn.setTransactionIsolation(isolationLevel);
            conn.setAutoCommit(false);  // 手动管理事务
            try (Statement stmt = conn.createStatement()) {
                System.out.println("事务A开始执行...");

                // 插入一条新数据
                stmt.executeUpdate("INSERT INTO employees (name, role, salary) VALUES ('Eve', 'Developer', 7000.00)");
                // 更新Bob的薪水
                stmt.executeUpdate("UPDATE employees SET salary = salary + 500 WHERE name = 'Bob'");

                // 打印事务A执行后的数据
                printEmployees(stmt);

                // 暂不提交事务
                System.out.println("事务A操作完成，但未提交...");
                Thread.sleep(5000);  // 模拟事务等待，延迟5秒

                // 提交事务
                conn.commit();
                System.out.println("事务A提交完成。");

            } catch (SQLException | InterruptedException e) {
                System.err.println("事务A发生错误，回滚。");
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 事务B：查询操作
    private static void transactionB(ComboPooledDataSource cpds, int isolationLevel) {
        try (Connection conn = cpds.getConnection()) {
            // 设置事务隔离级别
            conn.setTransactionIsolation(isolationLevel);
            conn.setAutoCommit(false);  // 手动管理事务
            try (Statement stmt = conn.createStatement()) {
                System.out.println("事务B开始执行查询操作...");

                // 查询事务A未提交时的数据
                printEmployees(stmt);

                // 提交事务B
                conn.commit();
                System.out.println("事务B提交完成。");

            } catch (SQLException e) {
                System.err.println("事务B发生错误，回滚。");
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * DML查询
     * **/
    // 基本DML操作：INSERT、UPDATE、DELETE
    private static void executeDML(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 插入一条新的员工记录
            stmt.executeUpdate("INSERT INTO employees (name, role, salary, department_id) " +
                    "VALUES ('Eve', 'Developer', 7000.00, 1)");

            // 更新员工Bob的薪水
            stmt.executeUpdate("UPDATE employees SET salary = 6500.00 WHERE name = 'Bob'");

            // 删除John的记录
            stmt.executeUpdate("DELETE FROM employees WHERE name = 'John'");

            System.out.println("DML操作完成。");

            // 打印当前表数据
            printEmployees(stmt);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 执行复杂查询、嵌套查询、多表联查
    private static void executeComplexQuery(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 嵌套查询：查找薪水大于所有HR员工的员工
            String nestedQuery = "SELECT name, salary FROM employees WHERE salary > " +
                    "(SELECT MAX(salary) FROM employees WHERE role = 'Engineer')";
            ResultSet rs = stmt.executeQuery(nestedQuery);
            System.out.println("嵌套查询结果：");
            while (rs.next()) {
                System.out.println(rs.getString("name") + " - " + rs.getDouble("salary"));
            }

            // 多表联查：查找每个员工及其部门名称
            String joinQuery = "SELECT e.name, e.role, e.salary, d.name AS department FROM employees e " +
                    "JOIN departments d ON e.department_id = d.id";
            rs = stmt.executeQuery(joinQuery);
            System.out.println("多表联查结果：");
            while (rs.next()) {
                System.out.println(rs.getString("name") + " - " + rs.getString("role") + " - " +
                        rs.getDouble("salary") + " - " + rs.getString("department"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 执行联合查询（UNION、INTERSECT、EXCEPT）和子查询
    private static void executeMultiQuery(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // UNION 操作：查找所有工程师和经理的名字
            String unionQuery = "SELECT name FROM employees WHERE role = 'Engineer' " +
                    "UNION SELECT name FROM employees WHERE role = 'Manager'";
            ResultSet rs = stmt.executeQuery(unionQuery);
            System.out.println("UNION 查询结果：");
            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }

            // 嵌套查询：查找薪水超过平均薪水的员工
            String subQuery = "SELECT name, salary FROM employees WHERE salary > " +
                    "(SELECT AVG(salary) FROM employees)";
            rs = stmt.executeQuery(subQuery);
            System.out.println("子查询结果：");
            while (rs.next()) {
                System.out.println(rs.getString("name") + " - " + rs.getDouble("salary"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * DDL操作+视图管理
     * **/
    // DDL操作：创建、修改、删除表和索引
    private static void executeDDL(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建新表
            String createTable = "CREATE TABLE IF NOT EXISTS projects (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "start_date DATE)";
            stmt.execute(createTable);
            System.out.println("表 'projects' 创建成功。");

            // 修改表：添加新列
            String alterTable = "ALTER TABLE projects ADD COLUMN end_date DATE";
            stmt.execute(alterTable);
            System.out.println("表 'projects' 修改成功，添加新列 'end_date'。");

            // 创建索引
            String createIndex = "CREATE INDEX idx_name ON projects (name)";
            stmt.execute(createIndex);
            System.out.println("索引 'idx_name' 创建成功。");

            // 删除索引
            String dropIndex = "DROP INDEX idx_name ON projects";
            stmt.execute(dropIndex);
            System.out.println("索引 'idx_name' 删除成功。");

            // 删除表
            String dropTable = "DROP TABLE IF EXISTS projects";
            stmt.execute(dropTable);
            System.out.println("表 'projects' 删除成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 视图管理：创建、更新、删除和查询视图
    private static void executeView(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建视图
            String createView = "CREATE VIEW employee_salaries AS " +
                    "SELECT name, salary FROM employees WHERE salary > 5000";
            stmt.execute(createView);
            System.out.println("视图 'employee_salaries' 创建成功。");

            // 查询视图数据
            System.out.println("查询视图 'employee_salaries' 的数据：");
            ResultSet rs = stmt.executeQuery("SELECT * FROM employee_salaries");
            while (rs.next()) {
                System.out.println("Name: " + rs.getString("name") + ", Salary: " + rs.getDouble("salary"));
            }

            // 更新视图（实际上不能直接修改视图，只能删除并重建）
            stmt.execute("DROP VIEW IF EXISTS employee_salaries");
            String recreateView = "CREATE VIEW employee_salaries AS " +
                    "SELECT name, salary FROM employees WHERE salary > 6500";
            stmt.execute(recreateView);
            System.out.println("视图 'employee_salaries' 更新成功。");

            // 再次查询视图数据
            System.out.println("查询更新后的视图 'employee_salaries' 的数据：");
            rs = stmt.executeQuery("SELECT * FROM employee_salaries");
            while (rs.next()) {
                System.out.println("Name: " + rs.getString("name") + ", Salary: " + rs.getDouble("salary"));
            }

            // 删除视图
            String dropView = "DROP VIEW IF EXISTS employee_salaries";
            stmt.execute(dropView);
            System.out.println("视图 'employee_salaries' 删除成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * DCL操作
     * **/
    // 测试用户权限管理）
    private static void userPermission(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建新用户
            String createUser = "CREATE USER 'test_user'@'localhost' IDENTIFIED BY '123456'";
            stmt.execute(createUser);
            System.out.println("用户 'test_user' 创建成功。");

            // 授予 SELECT 权限
            String grant = "GRANT SELECT ON employees TO 'test_user'@'localhost'";
            stmt.execute(grant);
            System.out.println("授予用户 'test_user' 对表 'employees' 的 SELECT 权限。");


            // 撤销权限
            String revoke = "REVOKE SELECT ON employees FROM 'test_user'@'localhost'";
            stmt.execute(revoke);
            System.out.println("撤销用户 'test_user' 对表 'employees' 的 SELECT 权限。");

            // 删除用户
            String dropUser = "DROP USER IF EXISTS 'test_user'@'localhost'";
            stmt.execute(dropUser);
            System.out.println("用户 'test_user' 删除成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 测试角色管理操作
    private static void roleManagement(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建角色
            String createRole = "CREATE ROLE read_only";
            stmt.execute(createRole);
            System.out.println("角色 'read_only' 创建成功。");

            // 授予角色权限
            String grantRole = "GRANT SELECT ON employees TO read_only";
            stmt.execute(grantRole);
            System.out.println("授予角色 'read_only' 对表 'employees' 的 SELECT 权限。");

            String createUser = "CREATE USER 'test_user'@'localhost' IDENTIFIED BY '123456'";
            stmt.execute(createUser);
            // 将角色分配给用户
            String assignRole = "GRANT read_only TO 'test_user'@'localhost'";
            stmt.execute(assignRole);
            System.out.println("将角色 'read_only' 分配给用户 'test_user'。");

            // 修改角色权限（例如添加 INSERT 权限）
            String alterRole = "GRANT INSERT ON employees TO read_only";
            stmt.execute(alterRole);
            System.out.println("为角色 'read_only' 添加 INSERT 权限。");

            // 撤销角色权限
            String revokeRolePermission = "REVOKE INSERT ON employees FROM read_only";
            stmt.execute(revokeRolePermission);
            System.out.println("撤销角色 'read_only' 对表 'employees' 的 INSERT 权限。");

            // 删除角色
            String dropRole = "DROP ROLE IF EXISTS read_only";
            stmt.execute(dropRole);
            System.out.println("角色 'read_only' 删除成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 聚合函数&窗口函数
     * **/
    private static void aggregation(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 执行聚合查询：计算员工人数、薪水总和、平均薪水、最小薪水、最大薪水
            String Query = "SELECT COUNT(*) AS employee_count, SUM(salary) AS total_salary, " +
                    "AVG(salary) AS average_salary, MIN(salary) AS min_salary, MAX(salary) AS max_salary " +
                    "FROM employees";
            ResultSet rs = stmt.executeQuery(Query);

            System.out.println("聚合函数结果：");
            if (rs.next()) {
                System.out.println("员工总数: " + rs.getInt("employee_count"));
                System.out.println("薪水总和: " + rs.getDouble("total_salary"));
                System.out.println("平均薪水: " + rs.getDouble("average_salary"));
                System.out.println("最低薪水: " + rs.getDouble("min_salary"));
                System.out.println("最高薪水: " + rs.getDouble("max_salary"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 窗口函数：ROW_NUMBER、RANK、DENSE_RANK
    private static void windowFun(ComboPooledDataSource cpds) {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 执行窗口函数查询：按薪水进行排序，并生成 ROW_NUMBER、RANK、DENSE_RANK
            String Query = "SELECT name, salary, " +
                    "ROW_NUMBER() OVER (ORDER BY salary DESC) AS row_num, " +
                    "RANK() OVER (ORDER BY salary DESC) AS rank_num, " +
                    "DENSE_RANK() OVER (ORDER BY salary DESC) AS dense_rank_num " +
                    "FROM employees";
            ResultSet rs = stmt.executeQuery(Query);

            System.out.println("窗口函数结果：");
            while (rs.next()) {
                System.out.println("姓名: " + rs.getString("name") +
                        ", 薪水: " + rs.getDouble("salary") +
                        ", ROW_NUMBER: " + rs.getInt("row_num") +
                        ", RANK: " + rs.getInt("rank_num") +
                        ", DENSE_RANK: " + rs.getInt("dense_rank_num"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 存储过程
     * **/
    private static void createStored(ComboPooledDataSource cpds) {
        String SQL =
                "CREATE PROCEDURE GetHighSalary(IN min_salary DECIMAL(10, 2))\n" +
                        "BEGIN\n" +
                        "    DECLARE done INT DEFAULT FALSE;\n" +
                        "    DECLARE emp_name VARCHAR(100);\n" +
                        "    DECLARE emp_salary DECIMAL(10, 2);\n" +
                        "    DECLARE cur CURSOR FOR \n" +
                        "        SELECT name, salary FROM employees WHERE salary > min_salary;\n" +
                        "    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;\n" +
                        "    OPEN cur;\n" +
                        "    read_loop: LOOP\n" +
                        "        FETCH cur INTO emp_name, emp_salary;\n" +
                        "        IF done THEN\n" +
                        "            LEAVE read_loop;\n" +
                        "        END IF;\n" +
                        "        SELECT emp_name, emp_salary;\n" +
                        "    END LOOP;\n" +
                        "    CLOSE cur;\n" +
                        "END;";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 执行存储过程创建
            stmt.execute(SQL);
            System.out.println("存储过程 'GetHighSalary' 创建成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void callStored(ComboPooledDataSource cpds, double minSalary) {
        String procedureCall = "{CALL GetHighSalary(?)}";  // 存储过程调用语句
        try (Connection conn = cpds.getConnection();
             CallableStatement stmt = conn.prepareCall(procedureCall)) {

            // 设置输入参数
            stmt.setDouble(1, minSalary);

            // 执行存储过程
            boolean hasResults = stmt.execute();

            // 处理结果集
            while (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    while (rs.next()) {
                        String empName = rs.getString(1);
                        double empSalary = rs.getDouble(2);
                        System.out.println("员工姓名: " + empName + ", 薪水: " + empSalary);
                    }
                }
                // 检查是否有更多的结果集
                hasResults = stmt.getMoreResults();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 分区表
     * **/
    private static void createPart(ComboPooledDataSource cpds) {
        String SQL =
                "CREATE TABLE employees_part (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    name VARCHAR(100)," +
                        "    role VARCHAR(100)," +
                        "    salary DECIMAL(10, 2)," +
                        "    department_id INT" +
                        ") " +
                        "PARTITION BY HASH(id % 2) (" +
                        "    PARTITION p_odd," +
                        "    PARTITION p_even" +
                        ");";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 执行分区表创建
            stmt.execute(SQL);
            System.out.println("分区表 'employees_part' 创建成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 向分区表插入数据
    private static void insertPart(ComboPooledDataSource cpds) {
        String insertSQL = "INSERT INTO employees_part (name, role, salary, department_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            // 插入数据
            pstmt.setString(1, "Alice");
            pstmt.setString(2, "Engineer");
            pstmt.setDouble(3, 5000.00);
            pstmt.setInt(4, 1);
            pstmt.executeUpdate();

            pstmt.setString(1, "Bob");
            pstmt.setString(2, "Manager");
            pstmt.setDouble(3, 6500.00);
            pstmt.setInt(4, 3);
            pstmt.executeUpdate();

            pstmt.setString(1, "Eve");
            pstmt.setString(2, "Developer");
            pstmt.setDouble(3, 7000.00);
            pstmt.setInt(4, 1);
            pstmt.executeUpdate();

            pstmt.setString(1, "John");
            pstmt.setString(2, "Intern");
            pstmt.setDouble(3, 3000.00);
            pstmt.setInt(4, 2);
            pstmt.executeUpdate();

            System.out.println("数据插入成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询分区表中的数据
    private static void queryPart(ComboPooledDataSource cpds) {
        String querySQL = "SELECT * FROM employees_part";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL);
             ResultSet rs = pstmt.executeQuery()) {

            // 查询并输出数据
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                double salary = rs.getDouble("salary");
                int department_id = rs.getInt("department_id");

                System.out.println("ID: " + id + ", Name: " + name + ", Role: " + role + ", Salary: " + salary + ", Department ID: " + department_id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 触发器
     * **/
    // 创建触发器
    private static void createTrigger(ComboPooledDataSource cpds) {
        String logTable =
                "CREATE TABLE IF NOT EXISTS audit_log (" +
                        "    id INT AUTO_INCREMENT PRIMARY KEY," +
                        "    employee_name VARCHAR(100)," +
                        "    action_type VARCHAR(10)," +
                        "    action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");";

        String SQL =
                "CREATE TRIGGER before_insert_employees " +
                        "BEFORE INSERT ON employees " +
                        "FOR EACH ROW " +
                        "BEGIN " +
                        "    INSERT INTO audit_log (employee_name, action_type) " +
                        "    VALUES (NEW.name, 'INSERT'); " +
                        "END;";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 创建审计日志表
            stmt.execute(logTable);
            System.out.println("表 'audit_log' 创建成功。");

            // 执行触发器创建
            stmt.execute(SQL);
            System.out.println("触发器 'before_insert_employees' 创建成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // 向 employees 表插入数据（将触发触发器）
    private static void insert(ComboPooledDataSource cpds) {
        String insertSQL = "INSERT INTO employees (name, role, salary, department_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, "Alice");
            pstmt.setString(2, "Engineer");
            pstmt.setDouble(3, 5000.00);
            pstmt.setInt(4, 1);
            pstmt.executeUpdate();

            System.out.println("数据插入成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询审计日志表以验证触发器的执行
    private static void queryAuditLog(ComboPooledDataSource cpds) {
        String querySQL = "SELECT * FROM audit_log";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL);
             ResultSet rs = pstmt.executeQuery()) {

            // 查询并输出审计日志数据
            while (rs.next()) {
                int id = rs.getInt("id");
                String employeeName = rs.getString("employee_name");
                String actionType = rs.getString("action_type");
                String actionTime = rs.getString("action_time");

                System.out.println("ID: " + id + ", Employee Name: " + employeeName +
                        ", Action Type: " + actionType + ", Action Time: " + actionTime);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * 用户定义函数
     * **/
    // 创建标量函数和表值函数
    private static void createFunctions(ComboPooledDataSource cpds) {
        String scalar =
                "CREATE FUNCTION calculate_tax(salary DECIMAL(10, 2)) " +
                        "RETURNS DECIMAL(10, 2) DETERMINISTIC " +
                        "BEGIN " +
                        "    DECLARE tax DECIMAL(10, 2); " +
                        "    SET tax = salary * 0.1; " +  // 假设税率为10%
                        "    RETURN tax; " +
                        "END;";

        String table =
                "CREATE PROCEDURE get_employees() " +
                        "BEGIN " +
                        "    SELECT id, name, role, salary FROM employees; " +
                        "END;";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 创建标量函数
            stmt.execute(scalar);
            System.out.println("标量函数 'calculate_tax' 创建成功。");

            // 创建表值函数（存储过程模拟）
            stmt.execute(table);
            System.out.println("存储过程 'get_employees' 创建成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    // 调用标量函数（计算税金）
    private static void callScalar(ComboPooledDataSource cpds) {
        String querySQL = "SELECT calculate_tax(?) AS tax";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            pstmt.setDouble(1, 5000.00);  // 计算5000.00薪资的税金

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double tax = rs.getDouble("tax");
                System.out.println("税金: " + tax);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 调用表值函数（存储过程模拟返回员工信息）
    private static void callTable(ComboPooledDataSource cpds) {
        String procedureCall = "{CALL get_employees()}";  // 调用存储过程
        try (Connection conn = cpds.getConnection();
             CallableStatement stmt = conn.prepareCall(procedureCall)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                double salary = rs.getDouble("salary");

                System.out.println("ID: " + id + ", Name: " + name + ", Role: " + role + ", Salary: " + salary);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * CTE
     * **/

    // 执行CTE递归查询
    private static void CTEQuery(ComboPooledDataSource cpds) {
        String cteQuery =
                "WITH RECURSIVE employee_hierarchy AS ( " +
                        "    SELECT id, name, role, manager_id, 1 AS level " +
                        "    FROM employees " +
                        "    WHERE id = ? " +  // 从指定员工开始递归查询
                        "    UNION ALL " +
                        "    SELECT e.id, e.name, e.role, e.manager_id, eh.level + 1 " +
                        "    FROM employees e " +
                        "    INNER JOIN employee_hierarchy eh ON e.manager_id = eh.id " +
                        ") " +
                        "SELECT * FROM employee_hierarchy";

        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(cteQuery)) {

            // 设置查询起始员工ID
            pstmt.setInt(1, 2);  // 从 Alice (id=1) 开始

            // 执行查询并输出结果
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                int managerId = rs.getInt("manager_id");
                int level = rs.getInt("level");

                System.out.println("ID: " + id + ", Name: " + name + ", Role: " + role +
                        ", Manager ID: " + managerId + ", Level: " + level);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * JSON & XML
     * **/

    // 插入JSON数据
    private static void insertJSON(ComboPooledDataSource cpds) {
        String insertSQL = "INSERT INTO employees_json (name, role, salary, profile) VALUES (?, ?, ?, JSON_OBJECT('address', JSON_OBJECT('city', ?, 'street', ?), 'phone', ?))";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, "Alice");
            pstmt.setString(2, "Engineer");
            pstmt.setDouble(3, 5000.00);
            pstmt.setString(4, "New York");
            pstmt.setString(5, "5th Avenue");
            pstmt.setString(6, "123-456-7890");
            pstmt.executeUpdate();

            System.out.println("JSON数据插入成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询JSON数据
    private static void queryJSON(ComboPooledDataSource cpds) {
        String querySQL = "SELECT name, role, profile->'$.address.city' AS city, profile->'$.phone' AS phone FROM employees_json";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String role = rs.getString("role");
                String city = rs.getString("city");
                String phone = rs.getString("phone");

                System.out.println("Name: " + name + ", Role: " + role + ", City: " + city + ", Phone: " + phone);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 插入XML数据
    private static void insertXML(ComboPooledDataSource cpds) {
        String insertSQL = "INSERT INTO employees_xml (name, role, salary, info) VALUES (?, ?, ?, ?)";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, "Bob");
            pstmt.setString(2, "Manager");
            pstmt.setDouble(3, 6500.00);
            pstmt.setString(4, "<employee><address><city>Los Angeles</city><street>Main Street</street></address><phone>987-654-3210</phone></employee>");
            pstmt.executeUpdate();

            System.out.println("XML数据插入成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询XML数据
    private static void queryXML(ComboPooledDataSource cpds) {
        String querySQL = "SELECT name, role, EXTRACTVALUE(info, '/employee/address/city') AS city, EXTRACTVALUE(info, '/employee/phone') AS phone FROM employees_xml";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String role = rs.getString("role");
                String city = rs.getString("city");
                String phone = rs.getString("phone");

                System.out.println("Name: " + name + ", Role: " + role + ", City: " + city + ", Phone: " + phone);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 数据导入
     * **/
    private static void importData(ComboPooledDataSource cpds) {
        String importSQL = "LOAD DATA LOCAL INFILE '" + "D:/code/ospp/src/employees.csv" + "' " +
                "INTO TABLE employees " +
                "FIELDS TERMINATED BY ',' " +
                "(id, name, role, salary, department_id)";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 执行批量导入
            stmt.execute(importSQL);
            System.out.println("数据导入成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 执行数据导出
    private static void exportData(ComboPooledDataSource cpds) {
        String filePath = new String("C:/ProgramData/MySQL/MySQL Server 8.4/Uploads/test.csv");
        String exportSQL = "SELECT id, name, role, salary, department_id " +
                "INTO OUTFILE '" + filePath + "' " +
                "FIELDS TERMINATED BY ',' " +
                "LINES TERMINATED BY '\n' " +
                "FROM employees";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 执行数据导出
            stmt.execute(exportSQL);
            System.out.println("数据导出成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}
