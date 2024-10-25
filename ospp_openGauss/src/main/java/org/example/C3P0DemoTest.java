package org.example;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class C3P0DemoTest {

    private ComboPooledDataSource cpds;

    @BeforeEach
    public void setUp() throws Exception {
        cpds = new ComboPooledDataSource();
        cpds.setDriverClass("org.opengauss.Driver");
        cpds.setJdbcUrl("jdbc:opengauss://192.168.56.102:7654/postgres");
        cpds.setUser("new_user");
        cpds.setPassword("hydrogen0923!");
        cpds.setInitialPoolSize(10);
        cpds.setMinPoolSize(10);
        cpds.setMaxPoolSize(50);
        cpds.setAcquireIncrement(5);
        cpds.setCheckoutTimeout(20000);
        cpds.setMaxIdleTime(60);
    }

    @AfterEach
    public void tearDown() throws Exception {
        cpds.close();
    }

    @Test
    public void testConnection() throws SQLException {
        try (Connection conn = cpds.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    /**
     * 事务管理
     * **/
    @Test
    public void testReadCommitted() throws InterruptedException, SQLException {
        System.out.println("Running with READ COMMITTED isolation level:");
        runTransactions(cpds, Connection.TRANSACTION_READ_COMMITTED);

        // 验证事务A提交后的预期结果
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            List<String> result = getEmployees(stmt);
            List<String> expected = List.of(
                    "ID: 1, Name: John, Role: Senior Engineer, Salary: 0.0",
                    "ID: 2, Name: Alice, Role: Engineer, Salary: 5000.0",
                    "ID: 3, Name: Bob, Role: Manager, Salary: 5000.0", // Bob's salary after increment
                    "ID: 4, Name: John, Role: HR, Salary: 4000.0",
                    "ID: 11, Name: Eve, Role: Developer, Salary: 7000.0"
            );
            assertEquals(expected, result);
        }
    }

    @Test
    public void testRepeatableRead() throws InterruptedException, SQLException {
        System.out.println("Running with REPEATABLE READ isolation level:");
        runTransactions(cpds, Connection.TRANSACTION_REPEATABLE_READ);

        // 验证事务A提交后的预期结果
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            List<String> result = getEmployees(stmt);
            List<String> expected = List.of(
                    "ID: 1, Name: John, Role: Senior Engineer, Salary: 0.0",
                    "ID: 2, Name: Alice, Role: Engineer, Salary: 5000.0",
                    "ID: 3, Name: Bob, Role: Manager, Salary: 5000.0", // Bob's salary after increment
                    "ID: 4, Name: John, Role: HR, Salary: 4000.0",
                    "ID: 11, Name: Eve, Role: Developer, Salary: 7000.0",
                    "ID: 12, Name: Eve, Role: Developer, Salary: 7000.0" // Duplicated Eve record
            );
            assertEquals(expected, result);
        }
    }

    @Test
    public void testSerializable() throws InterruptedException, SQLException {
        System.out.println("Running with SERIALIZABLE isolation level:");
        runTransactions(cpds, Connection.TRANSACTION_SERIALIZABLE);

        // 验证事务A提交后的预期结果
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            List<String> result = getEmployees(stmt);
            List<String> expected = List.of(
                    "ID: 1, Name: John, Role: Senior Engineer, Salary: 0.0",
                    "ID: 2, Name: Alice, Role: Engineer, Salary: 5000.0",
                    "ID: 3, Name: Bob, Role: Manager, Salary: 5500.0", // Bob's salary after multiple increments
                    "ID: 4, Name: John, Role: HR, Salary: 4000.0",
                    "ID: 11, Name: Eve, Role: Developer, Salary: 7000.0",
                    "ID: 12, Name: Eve, Role: Developer, Salary: 7000.0",
                    "ID: 13, Name: Eve, Role: Developer, Salary: 7000.0" // Triple Eve record
            );
            assertEquals(expected, result);
        }
    }

    private static void runTransactions(ComboPooledDataSource cpds, int isolationLevel) throws InterruptedException {
        Thread transactionA = new Thread(() -> transactionA(cpds, isolationLevel));
        Thread transactionB = new Thread(() -> transactionB(cpds, isolationLevel));

        transactionA.start();
        Thread.sleep(1000);
        transactionB.start();

        transactionA.join();
        transactionB.join();
    }

    private static void transactionA(ComboPooledDataSource cpds, int isolationLevel) {
        try (Connection conn = cpds.getConnection()) {
            conn.setTransactionIsolation(isolationLevel);
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO employees (name, role, salary) VALUES ('Eve', 'Developer', 7000.00)");
                stmt.executeUpdate("UPDATE employees SET salary = salary + 500 WHERE name = 'Bob'");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void transactionB(ComboPooledDataSource cpds, int isolationLevel) {
        try (Connection conn = cpds.getConnection()) {
            conn.setTransactionIsolation(isolationLevel);
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                getEmployees(stmt);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getEmployees(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT id, name, role, salary FROM employees");
        List<String> employees = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String role = rs.getString("role");
            double salary = rs.getDouble("salary");
            employees.add(String.format("ID: %d, Name: %s, Role: %s, Salary: %.2f", id, name, role, salary));
        }
        return employees;
    }

    /**
     * DML查询
     * **/
    @Test
    public void testDMLAndQueries() throws SQLException {
        // 执行DML操作并验证
        executeDML(cpds);

        // 执行复杂查询并验证
        executeComplexQuery(cpds);

        // 执行联合查询和子查询并验证
        executeMultiQuery(cpds);
    }

    private static void executeDML(ComboPooledDataSource cpds) throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 插入、更新和删除操作
            stmt.executeUpdate("INSERT INTO employees (name, role, salary, department_id) " +
                    "VALUES ('Eve', 'Developer', 7000.00, 1)");
            stmt.executeUpdate("UPDATE employees SET salary = 6500.00 WHERE name = 'Bob'");
            stmt.executeUpdate("DELETE FROM employees WHERE name = 'John'");

            System.out.println("DML操作完成。");

            // 验证当前表数据
            List<String> actual = getEmployees(stmt);
            List<String> expected = List.of(
                    "ID: 1, Name: Alice, Role: Engineer, Salary: 5000.0",
                    "ID: 2, Name: Bob, Role: Manager, Salary: 6500.0",
                    "ID: 4, Name: Eve, Role: Developer, Salary: 7000.0"
            );
            assertEquals(expected, actual);
        }
    }

    private static void executeComplexQuery(ComboPooledDataSource cpds) throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 嵌套查询
            String nestedQuery = "SELECT name, salary FROM employees WHERE salary > " +
                    "(SELECT MAX(salary) FROM employees WHERE role = 'Engineer')";
            ResultSet rs = stmt.executeQuery(nestedQuery);

            List<String> actualNestedResult = new ArrayList<>();
            while (rs.next()) {
                actualNestedResult.add(rs.getString("name") + " - " + rs.getDouble("salary"));
            }
            List<String> expectedNestedResult = List.of("Bob - 6500.0", "Eve - 7000.0");
            assertEquals(expectedNestedResult, actualNestedResult);

            // 多表联查
            String joinQuery = "SELECT e.name, e.role, e.salary, d.name AS department FROM employees e " +
                    "JOIN departments d ON e.department_id = d.id";
            rs = stmt.executeQuery(joinQuery);

            List<String> actualJoinResult = new ArrayList<>();
            while (rs.next()) {
                actualJoinResult.add(rs.getString("name") + " - " + rs.getString("role") + " - " +
                        rs.getDouble("salary") + " - " + rs.getString("department"));
            }
            List<String> expectedJoinResult = List.of(
                    "Alice - Engineer - 5000.0 - Engineering",
                    "Bob - Manager - 6500.0 - Management",
                    "Eve - Developer - 7000.0 - Engineering"
            );
            assertEquals(expectedJoinResult, actualJoinResult);
        }
    }

    private static void executeMultiQuery(ComboPooledDataSource cpds) throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // UNION 查询
            String unionQuery = "SELECT name FROM employees WHERE role = 'Engineer' " +
                    "UNION SELECT name FROM employees WHERE role = 'Manager'";
            ResultSet rs = stmt.executeQuery(unionQuery);

            List<String> actualUnionResult = new ArrayList<>();
            while (rs.next()) {
                actualUnionResult.add(rs.getString("name"));
            }
            List<String> expectedUnionResult = List.of("Alice", "Bob");
            assertEquals(expectedUnionResult, actualUnionResult);

            // 子查询
            String subQuery = "SELECT name, salary FROM employees WHERE salary > " +
                    "(SELECT AVG(salary) FROM employees)";
            rs = stmt.executeQuery(subQuery);

            List<String> actualSubQueryResult = new ArrayList<>();
            while (rs.next()) {
                actualSubQueryResult.add(rs.getString("name") + " - " + rs.getDouble("salary"));
            }
            List<String> expectedSubQueryResult = List.of("Bob - 6500.0", "Eve - 7000.0");
            assertEquals(expectedSubQueryResult, actualSubQueryResult);
        }
    }


    /**
     * DDL操作+视图管理
     * **/
    @Test
    public void testDDLAndView() throws SQLException {
        // 测试DDL操作
        executeDDL(cpds);

        // 测试视图管理操作
        executeView(cpds);
    }

    private static void executeDDL(ComboPooledDataSource cpds) throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建表
            String createTable = "CREATE TABLE IF NOT EXISTS projects (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(100), " +
                    "start_date DATE)";
            stmt.execute(createTable);
            System.out.println("表 'projects' 创建成功。");

            // 修改表
            String alterTable = "ALTER TABLE projects ADD COLUMN end_date DATE";
            stmt.execute(alterTable);
            System.out.println("表 'projects' 修改成功，添加新列 'end_date'。");

            // 创建索引
            String createIndex = "CREATE INDEX idx_name ON projects (name)";
            stmt.execute(createIndex);
            System.out.println("索引 'idx_name' 创建成功。");

            // 验证索引是否成功创建
            ResultSet rs = stmt.executeQuery("SELECT indexname FROM pg_indexes WHERE tablename = 'projects'");
            List<String> indexes = new ArrayList<>();
            while (rs.next()) {
                indexes.add(rs.getString("indexname"));
            }
            assertTrue(indexes.contains("idx_name"));

            // 删除索引
            String dropIndex = "DROP INDEX idx_name";
            stmt.execute(dropIndex);
            System.out.println("索引 'idx_name' 删除成功。");

            // 删除表
            String dropTable = "DROP TABLE IF EXISTS projects";
            stmt.execute(dropTable);
            System.out.println("表 'projects' 删除成功。");
        }
    }

    private static void executeView(ComboPooledDataSource cpds) throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建视图
            String createView = "CREATE VIEW employee_salaries AS " +
                    "SELECT name, salary FROM employees WHERE salary > 5000";
            stmt.execute(createView);
            System.out.println("视图 'employee_salaries' 创建成功。");

            // 验证视图的创建并查询数据
            System.out.println("查询视图 'employee_salaries' 的数据：");
            List<String> actualFirstViewData = queryView(stmt);
            List<String> expectedFirstViewData = List.of("Bob - 6500.0", "Eve - 7000.0");
            assertEquals(expectedFirstViewData, actualFirstViewData);

            // 更新视图
            stmt.execute("DROP VIEW IF EXISTS employee_salaries");
            String recreateView = "CREATE VIEW employee_salaries AS " +
                    "SELECT name, salary FROM employees WHERE salary > 6500";
            stmt.execute(recreateView);
            System.out.println("视图 'employee_salaries' 更新成功。");

            // 查询更新后的视图
            System.out.println("查询更新后的视图 'employee_salaries' 的数据：");
            List<String> actualUpdatedViewData = queryView(stmt);
            List<String> expectedUpdatedViewData = List.of("Eve - 7000.0");
            assertEquals(expectedUpdatedViewData, actualUpdatedViewData);

            // 删除视图
            String dropView = "DROP VIEW IF EXISTS employee_salaries";
            stmt.execute(dropView);
            System.out.println("视图 'employee_salaries' 删除成功。");
        }
    }

    private static List<String> queryView(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT * FROM employee_salaries");
        List<String> viewData = new ArrayList<>();
        while (rs.next()) {
            String name = rs.getString("name");
            double salary = rs.getDouble("salary");
            viewData.add(name + " - " + salary);
        }
        return viewData;
    }
    /**
     * DCL操作
     * **/
    @Test
    public void testUserPermissionManagement() throws SQLException {
        userPermission(cpds);
        roleManagement(cpds);
    }

    private static void userPermission(ComboPooledDataSource cpds) throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建新用户
            String createUser = "CREATE USER test_user WITH PASSWORD 'lvxun666@'";
            stmt.execute(createUser);
            System.out.println("用户 'test_user' 创建成功。");

            // 验证用户是否创建成功
            ResultSet rs = stmt.executeQuery("SELECT * FROM pg_roles WHERE rolname = 'test_user'");
            assertTrue(rs.next(), "用户 'test_user' 应该已被创建");

            // 授予 SELECT 权限
            String grant = "GRANT SELECT ON employees TO test_user";
            stmt.execute(grant);
            System.out.println("授予用户 'test_user' 对表 'employees' 的 SELECT 权限。");

            // 验证用户权限
            rs = stmt.executeQuery("SELECT has_table_privilege('test_user', 'employees', 'SELECT')");
            assertTrue(rs.next() && rs.getBoolean(1), "用户 'test_user' 应该拥有对表 'employees' 的 SELECT 权限");

            // 撤销权限
            String revoke = "REVOKE SELECT ON employees FROM test_user";
            stmt.execute(revoke);
            System.out.println("撤销用户 'test_user' 对表 'employees' 的 SELECT 权限。");

            // 验证用户权限已撤销
            rs = stmt.executeQuery("SELECT has_table_privilege('test_user', 'employees', 'SELECT')");
            assertTrue(rs.next() && !rs.getBoolean(1), "用户 'test_user' 应该没有对表 'employees' 的 SELECT 权限");

            // 删除用户
            String dropUser = "DROP USER IF EXISTS test_user";
            stmt.execute(dropUser);
            System.out.println("用户 'test_user' 删除成功。");

            // 验证用户是否已删除
            rs = stmt.executeQuery("SELECT * FROM pg_roles WHERE rolname = 'test_user'");
            assertFalse(rs.next(), "用户 'test_user' 应该已被删除");
        }
    }

    private static void roleManagement(ComboPooledDataSource cpds) throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 创建角色
            String createRole = "CREATE ROLE read_only WITH PASSWORD 'lvxun666@'";
            stmt.execute(createRole);
            System.out.println("角色 'read_only' 创建成功。");

            // 验证角色是否创建成功
            ResultSet rs = stmt.executeQuery("SELECT * FROM pg_roles WHERE rolname = 'read_only'");
            assertTrue(rs.next(), "角色 'read_only' 应该已被创建");

            // 授予角色权限
            String grantRole = "GRANT SELECT ON employees TO read_only";
            stmt.execute(grantRole);
            System.out.println("授予角色 'read_only' 对表 'employees' 的 SELECT 权限。");

            // 创建用户
            String createUser = "CREATE USER test_user WITH PASSWORD 'lvxun666@'";
            stmt.execute(createUser);

            // 将角色分配给用户
            String assignRole = "GRANT read_only TO test_user";
            stmt.execute(assignRole);
            System.out.println("将角色 'read_only' 分配给用户 'test_user'。");

            // 验证用户是否拥有角色的权限
            rs = stmt.executeQuery("SELECT has_table_privilege('test_user', 'employees', 'SELECT')");
            assertTrue(rs.next() && rs.getBoolean(1), "用户 'test_user' 应该拥有角色 'read_only' 的 SELECT 权限");

            // 修改角色权限（例如添加 INSERT 权限）
            String alterRole = "GRANT INSERT ON employees TO read_only";
            stmt.execute(alterRole);
            System.out.println("为角色 'read_only' 添加 INSERT 权限。");

            // 验证角色权限是否添加成功
            rs = stmt.executeQuery("SELECT has_table_privilege('read_only', 'employees', 'INSERT')");
            assertTrue(rs.next() && rs.getBoolean(1), "角色 'read_only' 应该拥有对表 'employees' 的 INSERT 权限");

            // 撤销角色权限
            String revokeRolePermission = "REVOKE INSERT ON employees FROM read_only";
            stmt.execute(revokeRolePermission);
            System.out.println("撤销角色 'read_only' 对表 'employees' 的 INSERT 权限。");

            // 验证角色权限是否已撤销
            rs = stmt.executeQuery("SELECT has_table_privilege('read_only', 'employees', 'INSERT')");
            assertTrue(rs.next() && !rs.getBoolean(1), "角色 'read_only' 应该没有对表 'employees' 的 INSERT 权限");

            // 删除角色
            String dropRole = "DROP ROLE IF EXISTS read_only";
            stmt.execute(dropRole);
            System.out.println("角色 'read_only' 删除成功。");

            // 验证角色是否已删除
            rs = stmt.executeQuery("SELECT * FROM pg_roles WHERE rolname = 'read_only'");
            assertFalse(rs.next(), "角色 'read_only' 应该已被删除");
        }
    }
/**
 * 聚合函数&窗口函数
 * **/
@Test
public void testAggregation() throws SQLException {
    try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
        // 执行聚合查询
        String query = "SELECT COUNT(*) AS employee_count, SUM(salary) AS total_salary, " +
                "AVG(salary) AS average_salary, MIN(salary) AS min_salary, MAX(salary) AS max_salary " +
                "FROM employees";
        ResultSet rs = stmt.executeQuery(query);

        assertTrue(rs.next(), "聚合查询应该返回结果");
        assertEquals(3, rs.getInt("employee_count"), "员工总数应该为 3");
        assertEquals(18500.0, rs.getDouble("total_salary"), "薪水总和应该为 18500.0");
        assertEquals(6166.66667, rs.getDouble("average_salary"), 0.0001, "平均薪水应该为 6166.66667");
        assertEquals(5000.0, rs.getDouble("min_salary"), "最低薪水应该为 5000.0");
        assertEquals(7000.0, rs.getDouble("max_salary"), "最高薪水应该为 7000.0");
    }
}

    @Test
    public void testWindowFunction() throws SQLException {
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            // 执行窗口函数查询
            String query = "SELECT name, salary, " +
                    "ROW_NUMBER() OVER (ORDER BY salary DESC) AS row_num, " +
                    "RANK() OVER (ORDER BY salary DESC) AS rank_num, " +
                    "DENSE_RANK() OVER (ORDER BY salary DESC) AS dense_rank_num " +
                    "FROM employees";
            ResultSet rs = stmt.executeQuery(query);

            // 验证窗口函数结果
            assertTrue(rs.next(), "窗口函数查询应该返回结果");
            assertEquals("Eve", rs.getString("name"), "第一行的姓名应该是 Eve");
            assertEquals(7000.0, rs.getDouble("salary"), "第一行的薪水应该是 7000.0");
            assertEquals(1, rs.getInt("row_num"), "第一行的 ROW_NUMBER 应该为 1");
            assertEquals(1, rs.getInt("rank_num"), "第一行的 RANK 应该为 1");
            assertEquals(1, rs.getInt("dense_rank_num"), "第一行的 DENSE_RANK 应该为 1");

            assertTrue(rs.next(), "窗口函数查询应该返回结果");
            assertEquals("Bob", rs.getString("name"), "第二行的姓名应该是 Bob");
            assertEquals(6500.0, rs.getDouble("salary"), "第二行的薪水应该是 6500.0");
            assertEquals(2, rs.getInt("row_num"), "第二行的 ROW_NUMBER 应该为 2");
            assertEquals(2, rs.getInt("rank_num"), "第二行的 RANK 应该为 2");
            assertEquals(2, rs.getInt("dense_rank_num"), "第二行的 DENSE_RANK 应该为 2");

            assertTrue(rs.next(), "窗口函数查询应该返回结果");
            assertEquals("Alice", rs.getString("name"), "第三行的姓名应该是 Alice");
            assertEquals(5000.0, rs.getDouble("salary"), "第三行的薪水应该是 5000.0");
            assertEquals(3, rs.getInt("row_num"), "第三行的 ROW_NUMBER 应该为 3");
            assertEquals(3, rs.getInt("rank_num"), "第三行的 RANK 应该为 3");
            assertEquals(3, rs.getInt("dense_rank_num"), "第三行的 DENSE_RANK 应该为 3");
        }
    }

    @Test
    public void testProcedure() throws SQLException {
        // 创建存储过程
        createStored(cpds);

        // 调用存储过程并验证
        callStored(cpds, 1);

        // 进一步验证：查询员工的薪水
        try (Connection conn = cpds.getConnection(); Statement stmt = conn.createStatement()) {
            String query = "SELECT salary FROM employees WHERE id = 1";
            ResultSet rs = stmt.executeQuery(query);
            assertTrue(rs.next(), "员工 ID 1 应该存在");

            double salary = rs.getDouble("salary");
            System.out.println("员工 1 的薪水: " + salary);
            assertTrue(salary > 4000, "员工 1 的薪水应该大于 4000");
        }
    }

    private static void createStored(ComboPooledDataSource cpds) {
        String SQL = "CREATE OR REPLACE PROCEDURE check_salary(p_employee_id INT)\n" +
                " AS\n" +
                " DECLARE\n" +
                " \tv_salary DECIMAL(10, 2);\n" +
                " BEGIN\n" +
                " \tSELECT salary INTO v_salary FROM employees WHERE id = p_employee_id;\n" +
                " \tIF v_salary > 4000 THEN\n" +
                " \t\tRAISE NOTICE 'Employee % has a high salary: %', p_employee_id, v_salary;\n" +
                " \tELSE\n" +
                " \t\tRAISE NOTICE 'Employee % has a standard salary: %', p_employee_id, v_salary;\n" +
                " \tEND IF;\n" +
                " END;";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {
            // 执行存储过程创建
            stmt.execute(SQL);
            System.out.println("存储过程 'check_salary' 创建成功。");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void callStored(ComboPooledDataSource cpds, int id) {
        String procedureCall = "{CALL check_salary(?)}";  // 存储过程调用语句
        try (Connection conn = cpds.getConnection();
             CallableStatement stmt = conn.prepareCall(procedureCall)) {

            // 设置输入参数
            stmt.setInt(1, id);
            stmt.execute();  // 执行存储过程
            System.out.println("存储过程 'check_salary' 已经执行");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     *分区表
     **/
    @Test
    public void testPartitioned() throws SQLException {
        // 创建分区表
        createPart(cpds);

        // 插入数据
        insertPart(cpds);

        // 查询并验证数据
        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM employees_part");
            assertTrue(rs.next(), "应该返回一个结果");
            int count = rs.getInt("count");
            assertEquals(4, count, "应插入4条记录");

            // 查询并验证具体数据
            ResultSet queryRs = stmt.executeQuery("SELECT * FROM employees_part ORDER BY id");
            int[] expectedIds = {1, 2, 3, 4};
            String[] expectedNames = {"Alice", "Bob", "Eve", "John"};
            double[] expectedSalaries = {5000.00, 6500.00, 7000.00, 3000.00};

            int i = 0;
            while (queryRs.next()) {
                assertEquals(expectedIds[i], queryRs.getInt("id"), "ID应该匹配");
                assertEquals(expectedNames[i], queryRs.getString("name"), "名字应该匹配");
                assertEquals(expectedSalaries[i], queryRs.getDouble("salary"), "薪水应该匹配");
                i++;
            }
            assertEquals(4, i, "应有4条数据");

        }
    }

    private static void createPart(ComboPooledDataSource cpds) {
        String SQL =
                "CREATE TABLE employees_part (" +
                        "    id SERIAL PRIMARY KEY," +
                        "    name VARCHAR(100)," +
                        "    role VARCHAR(100)," +
                        "    salary DECIMAL(10, 2)," +
                        "    department_id INT" +
                        ") PARTITION BY HASH (id) " +
                        "( " +
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

    private static void insertPart(ComboPooledDataSource cpds) {
        String insertSQL = "INSERT INTO employees_part (name, role, salary, department_id) VALUES ( ?, ?, ?, ?)";
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
/**
 *触发器+表值函数
 */
@Test
public void testTriggerExecution() throws SQLException {
    // 创建触发器函数
    createTriggerFunction(cpds);

    // 创建触发器
    createTrigger(cpds);

    // 插入数据（将触发触发器）
    insert(cpds);

    // 查询并验证审计日志
    try (Connection conn = cpds.getConnection();
         Statement stmt = conn.createStatement()) {

        ResultSet rs = stmt.executeQuery("SELECT * FROM audit_log");

        assertTrue(rs.next(), "应该返回一个审计日志结果");
        String employeeName = rs.getString("employee_name");
        String actionType = rs.getString("action_type");

        assertEquals("Alice", employeeName, "插入的员工名称应该为Alice");
        assertEquals("INSERT", actionType, "操作类型应该是INSERT");

    }
}



    private static void createTriggerFunction(ComboPooledDataSource cpds) {
        String createFunctionSQL = "CREATE OR REPLACE FUNCTION log_employee_action() "
                + "RETURNS TRIGGER AS $$ "
                + "BEGIN "
                + "    INSERT INTO audit_log (employee_name, action_type) "
                + "    VALUES (NEW.name, 'INSERT'); "
                + "    RETURN NEW; "
                + "END; "
                + "$$ LANGUAGE plpgsql;";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createFunctionSQL);
            System.out.println("触发器函数 'log_employee_action' 创建成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTrigger(ComboPooledDataSource cpds) {
        String createTriggerSQL = "CREATE TRIGGER after_employee_insert "
                + "AFTER INSERT ON employees "
                + "FOR EACH ROW "
                + "EXECUTE PROCEDURE log_employee_action();";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTriggerSQL);
            System.out.println("触发器 'after_employee_insert' 创建成功。");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void insert(ComboPooledDataSource cpds) {
        String insertSQL = "INSERT INTO employees (name, role, salary, department_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            // 插入数据
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
    /**
     * 函数
     */
    @Test
    public void testCreateFunction() {
        String SQL =
                "CREATE OR REPLACE FUNCTION get_employee_salary(emp_id INT) " +
                        "RETURNS DECIMAL AS $$ " +
                        "BEGIN " +
                        "    RETURN (SELECT salary FROM employees WHERE id = emp_id); " +
                        "END; " +
                        "$$ LANGUAGE plpgsql;";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            // 执行函数创建
            stmt.execute(SQL);
            System.out.println("函数 'get_employee_salary' 创建成功。");
        } catch (SQLException e) {
            Assertions.fail("Failed to create function: " + e.getMessage());
        }
    }

    @Test
    public void testCallFunction() {
        int empId = 1;
        String functionCall = "{? = CALL get_employee_salary(?)}";
        double salary = 0.0;

        try (Connection conn = cpds.getConnection();
             CallableStatement stmt = conn.prepareCall(functionCall)) {

            // 注册输出参数
            stmt.registerOutParameter(1, java.sql.Types.DECIMAL);
            // 设置输入参数
            stmt.setInt(2, empId);

            // 执行函数
            stmt.execute();

            // 获取返回值，直接用 double 类型
            salary = stmt.getDouble(1);
            System.out.println("员工 ID: " + empId + ", 薪水: " + salary);

        } catch (SQLException e) {
            Assertions.fail("Failed to call function: " + e.getMessage());
        }

        Assertions.assertTrue(salary > 0, "Salary should be greater than 0.");
    }
    @Test
    public void testCTEQuery() {
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
            pstmt.setInt(1, 2);  // 从指定员工（例如 ID = 2）开始

            // 执行查询并输出结果
            ResultSet rs = pstmt.executeQuery();
            boolean found = false;

            while (rs.next()) {
                found = true;
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String role = rs.getString("role");
                int managerId = rs.getInt("manager_id");
                int level = rs.getInt("level");

                System.out.println("ID: " + id + ", Name: " + name + ", Role: " + role +
                        ", Manager ID: " + managerId + ", Level: " + level);
            }


            Assertions.assertTrue(found, "No employees found in the hierarchy for the given ID.");

        } catch (SQLException e) {
            Assertions.fail("Failed to execute CTE query: " + e.getMessage());
        }

    }
    @Test
    public void testInsertAndQueryJSON() {
        // 插入 JSON 数据
        String insertSQL = "INSERT INTO employees_json (name, role, salary, profile) VALUES (?, ?, ?, ?::json)";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, "Alice");
            pstmt.setString(2, "Engineer");
            pstmt.setDouble(3, 5000.00);
            String jsonProfile = String.format("{\"address\":{\"city\":\"%s\",\"street\":\"%s\"},\"phone\":\"%s\"}",
                    "New York", "5th Avenue", "123-456-7890");
            pstmt.setString(4, jsonProfile);
            pstmt.executeUpdate();

            System.out.println("JSON 数据插入成功。");

        } catch (SQLException e) {
            Assertions.fail("插入 JSON 数据失败: " + e.getMessage());
        }

        // 查询 JSON 数据
        String querySQL = "SELECT name, role, profile->'address'->>'city' AS city, profile->>'phone' AS phone FROM employees_json";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL);
             ResultSet rs = pstmt.executeQuery()) {

            boolean found = false; // 标志以检查是否找到数据

            while (rs.next()) {
                found = true; // 找到数据
                String name = rs.getString("name");
                String role = rs.getString("role");
                String city = rs.getString("city");
                String phone = rs.getString("phone");

                System.out.println("Name: " + name + ", Role: " + role + ", City: " + city + ", Phone: " + phone);
            }

            // 断言至少找到一条记录
            Assertions.assertTrue(found, "未找到 JSON 数据。");

        } catch (SQLException e) {
            Assertions.fail("查询 JSON 数据失败: " + e.getMessage());
        }
    }
    @Test
    public void testInsertAndQueryXML() {
        // 插入 XML 数据
        String insertSQL = "INSERT INTO employees_xml (name, role, salary, info) VALUES (?, ?, ?, ?)";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, "Bob");
            pstmt.setString(2, "Manager");
            pstmt.setDouble(3, 6500.00);
            pstmt.setString(4, "<employee><address><city>Los Angeles</city><street>Main Street</street></address><phone>987-654-3210</phone></employee>");
            pstmt.executeUpdate();

            System.out.println("XML 数据插入成功。");

        } catch (SQLException e) {
            Assertions.fail("插入 XML 数据失败: " + e.getMessage());
        }

        // 查询 XML 数据
        String querySQL = "SELECT name, role, " +
                "SUBSTRING(info FROM '<city>(.*?)</city>') AS city, " +
                "SUBSTRING(info FROM '<phone>(.*?)</phone>') AS phone " +
                "FROM employees_xml";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL);
             ResultSet rs = pstmt.executeQuery()) {

            boolean found = false; // 标志以检查是否找到数据

            while (rs.next()) {
                found = true; // 找到数据
                String name = rs.getString("name");
                String role = rs.getString("role");
                String city = rs.getString("city");
                String phone = rs.getString("phone");

                System.out.println("Name: " + name + ", Role: " + role + ", City: " + city + ", Phone: " + phone);
            }

            // 断言至少找到一条记录
            Assertions.assertTrue(found, "未找到 XML 数据。");

        } catch (SQLException e) {
            Assertions.fail("查询 XML 数据失败: " + e.getMessage());
        }
    }
    /**
     * 外部表
     */
    @Test
    public void testCopyDataFromCSV() {
        String filePath = "/path/to/your/employees.csv"; // 替换为实际的CSV文件路径
        String copySQL = "COPY employees (name, role, salary, department_id) FROM '" + filePath + "' WITH (FORMAT CSV, HEADER TRUE)";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(copySQL);
            System.out.println("数据从 CSV 文件导入成功。");

            // 验证数据是否插入
            String querySQL = "SELECT COUNT(*) FROM employees"; // 计算表中的行数
            try (ResultSet rs = stmt.executeQuery(querySQL)) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    Assertions.assertTrue(count > 0, "未能从 CSV 文件导入数据。");
                }
            }

        } catch (SQLException e) {
            Assertions.fail("数据从 CSV 文件导入失败: " + e.getMessage());
        }
    }

    @Test
    public void testExportDataToCSV() {
        String filePath = "/path/to/your/exported_employees.csv"; // 替换为导出文件的路径
        String copySQL = "\\copy (SELECT * FROM employees) TO '" + filePath + "' WITH (FORMAT CSV, HEADER TRUE)";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(copySQL);
            System.out.println("数据导出到 CSV 文件成功。");

            // 验证导出文件是否存在
            File file = new File(filePath);
            Assertions.assertTrue(file.exists(), "导出的 CSV 文件不存在。");

        } catch (SQLException e) {
            Assertions.fail("数据导出到 CSV 文件失败: " + e.getMessage());
        }
    }

    @Test
    public void testCreateAndQueryExternalTable() {
        // 创建外部表
        String createSQL = "CREATE FOREIGN TABLE employees_external (\n" +
                "    id INT,\n" +
                "    name VARCHAR(100),\n" +
                "    role VARCHAR(100),\n" +
                "    salary DECIMAL(10, 2),\n" +
                "    department_id INT,\n" +
                "    manager_id INT\n" +
                ")\n" +
                "SERVER my_file_server\n" +
                "OPTIONS (\n" +
                "    filename '/var/lib/opengauss/employees.csv', \n" +
                "    format 'csv',  \n" +
                "    delimiter ',', \n" +
                "    null 'NULL' " +
                ");\n";

        try (Connection conn = cpds.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createSQL);
            System.out.println("外部表 'employees_external' 创建成功。");
        } catch (SQLException e) {
            Assertions.fail("创建外部表失败: " + e.getMessage());
        }

        // 查询外部表
        String querySQL = "SELECT * FROM employees_external";
        try (Connection conn = cpds.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySQL);
             ResultSet rs = pstmt.executeQuery()) {

            boolean found = false; // 标志以检查是否找到数据

            while (rs.next()) {
                found = true; // 找到数据
                String name = rs.getString("name");
                String role = rs.getString("role");
                double salary = rs.getDouble("salary");
                int departmentId = rs.getInt("department_id");

                System.out.println("Name: " + name + ", Role: " + role + ", Salary: " + salary + ", Department ID: " + departmentId);
            }

            // 断言至少找到一条记录
            Assertions.assertTrue(found, "未找到外部表中的数据。");

        } catch (SQLException e) {
            Assertions.fail("查询外部表失败: " + e.getMessage());
        }
    }






}

