import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TestIfElse {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("IF v_salary >= 100000 THEN\n" +
                "                 v_bonus := v_salary * 0.1;\n" +
                "             ELSIF v_salary >= 50000 THEN\n" +
                "                 v_bonus := v_salary * 0.08;\n" +
                "             ELSIF v_salary >= 30000 THEN\n" +
                "                 v_bonus := v_salary * 0.05;\n" +
                "             ELSE\n" +
                "                 v_bonus := v_salary * 0.03;\n" +
                "             END IF;");
        System.out.println("===== test of IfElse =====");
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
            ASTNode root = OracleParser.parseIFELSE(lexer.getTokens());
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
