import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TestProcedure {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("CREATE OR REPLACE PROCEDURE update_salary (\n" +
                "             employee_id IN NUMBER,\n" +
                "             new_salary IN OUT NUMBER\n" +
                "         ) IS\n" +
                "             v_employee employees%ROWTYPE;\n" +
                "         BEGIN\n" +
                "             SELECT * FROM employees WHERE employee_id = employee_id;\n" +
                "             IF new_salary < 3000 THEN\n" +
                "                 new_salary := new_salary * 1.1;\n" +
                "             ELSE\n" +
                "                 new_salary := new_salary * 1.05;\n" +
                "             END IF;\n" +
                "             UPDATE employees SET salary = new_salary WHERE employee_id = employee_id;\n" +
                "             COMMIT;\n" +
                "             DBMS_OUTPUT.PUT_LINE('Updated Salary: ' || new_salary);\n" +
                "         EXCEPTION\n" +
                "             WHEN NO_DATA_FOUND THEN\n" +
                "                 DBMS_OUTPUT.PUT_LINE('Employee not found.');\n" +
                "             WHEN TOO_MANY_ROWS THEN\n" +
                "                 DBMS_OUTPUT.PUT_LINE('Multiple employees found.');\n" +
                "             WHEN OTHERS THEN\n" +
                "                 DBMS_OUTPUT.PUT_LINE('An error occurred: ' || SQLERRM);\n" +
                "         END update_salary;");
        System.out.println("===== test of Procedure =====");
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
