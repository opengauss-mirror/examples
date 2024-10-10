import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TestView {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("CREATE OR REPLACE VIEW employee_details AS SELECT first_name, last_name, salary FROM employees;");
        testSQL.add("CREATE OR REPLACE VIEW emp_info (full_name, pay) AS SELECT first_name || ' ' || last_name AS full_name, salary AS pay FROM employees;");
        testSQL.add("CREATE OR REPLACE VIEW emp_dept_info AS SELECT e.first_name, e.last_name, d.department_name FROM employees e JOIN departments d ON e.department_id = d.department_id;");
        testSQL.add("CREATE OR REPLACE VIEW high_salary_employees AS SELECT first_name, last_name, salary FROM employees WHERE salary > 50000;");
        System.out.println("===== test of View =====");
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
