import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class testException {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("EXCEPTION\n" +
                "    WHEN e_custom_exception THEN\n" +
                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: Custom exception raised');\n" +
                "    WHEN ZERO_DIVIDE THEN\n" +
                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: Division by zero');\n" +
                "    WHEN INVALID_NUMBER THEN\n" +
                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: Invalid number');\n" +
                "    WHEN OTHERS THEN\n" +
                "        DBMS_OUTPUT.PUT_LINE('Caught an exception: ' || SQLERRM);");
        System.out.println("===== test of EXCEPTION =====");
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
            ASTNode root = OracleParser.parseException(lexer.getTokens());
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
