import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TestInsert {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("INSERT INTO employees (first_name, last_name, email) VALUES ('Jane', 'Smith', 'janesm@example.com');");
        testSQL.add("INSERT INTO employees VALUES ('Jane', 'Smith', 'janesm@example.com');");
        System.out.println("===== test of Insert =====");
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
