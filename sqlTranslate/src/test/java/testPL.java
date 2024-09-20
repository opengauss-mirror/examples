import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class testPL {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("DECLARE\n" +
                "            v_name Varchar2(20);\n" +
                "            v_salary employees.salary%TYPE;\n" +
                "         BEGIN\n" +
                "            SELECT name, salary INTO v_name, v_salary FROM employees WHERE employee_id = 100;\n" +
                "            DBMS_OUTPUT.PUT_LINE('Name: ' || v_name || ', Salary: ' || v_salary);\n" +
                "         EXCEPTION\n" +
                "            WHEN NO_DATA_FOUND THEN\n" +
                "               DBMS_OUTPUT.PUT_LINE('No data found.');\n" +
                "         END;");
        System.out.println("===== test of PL =====");
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
