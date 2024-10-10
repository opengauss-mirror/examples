import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TestSelect {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("SELECT * FROM employees Union SELECT employee_id, first_name, last_name FROM employees;");
        testSQL.add("SELECT * FROM employees WHERE department_id = 10;");
        testSQL.add("SELECT * FROM employees ORDER BY hire_date DESC;");
        testSQL.add("SELECT COUNT(*), AVG(salary) FROM employees;");
        testSQL.add("SELECT department_id, COUNT(*) FROM employees GROUP BY department_id;");
        testSQL.add("SELECT e.first_name, d.department_name FROM employees e JOIN departments d ON e.department_id = d.department_id;");
        testSQL.add("SELECT\n" +
                "        column1,\n" +
                "        CASE WHEN column2 > 0 THEN 'Positive' ELSE 'Non-positive' END as status,\n" +
                "        SUBSTR(column3, 1, 5) as substring_column3\n" +
                "    FROM table_name;");
        testSQL.add("SELECT column1, COUNT(column2) FROM table_name GROUP BY column1 HAVING COUNT(column2) > 10;");
        System.out.println("===== test of Select =====");
        System.out.println("The source DBMS is: " + CommonConfig.getSourceDB());
        System.out.println("The target DBMS is: " + CommonConfig.getTargetDB());
        System.out.println();
    }

    @Test
    public void test()
    {
        int num = 1;
        for (String sql : testSQL) {
            System.out.println("===== test the SQL" + num++ + " =====");
            System.out.println("Input SQL: " + sql);
            OracleLexer lexer = new OracleLexer(sql);
            lexer.printTokens();
            OracleParser parser = new OracleParser(lexer);
            ASTNode root = parser.parse();
            System.out.println("The AST of the input SQL: ");
            System.out.println(root.getASTString());
            System.out.println("The query String of the AST parsed from the input SQL: ");
            System.out.println(root.toQueryString());
            OpenGaussGenerator generator = new OpenGaussGenerator(root);
            System.out.println("The converted query String: ");
            System.out.println(generator.generate());

            System.out.println();
        }
    }
}
