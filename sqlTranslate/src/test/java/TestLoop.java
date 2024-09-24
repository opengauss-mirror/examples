import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TestLoop {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("LOOP\n" +
                "            DBMS_OUTPUT.PUT_LINE(v_counter);\n" +
                "            v_counter := v_counter + 1;\n" +
                "            EXIT WHEN v_counter > 10;\n" +
                "        END LOOP;");
        testSQL.add("WHILE v_counter <= 10 LOOP\n" +
                "            DBMS_OUTPUT.PUT_LINE(v_counter);\n" +
                "            v_counter := v_counter + 1;\n" +
                "        END LOOP;");
        testSQL.add("FOR i IN 1..10 LOOP\n" +
                "            DBMS_OUTPUT.PUT_LINE(i);\n" +
                "        END LOOP;");
        System.out.println("===== test of Loop =====");
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
            ASTNode root = OracleParser.parseLoop(lexer.getTokens());
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
