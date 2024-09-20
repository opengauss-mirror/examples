import config.CommonConfig;
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.OracleParser;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class testAlterTable {
    List<String> testSQL = new ArrayList<>();
    @BeforeEach
    public void loadData()
    {
        testSQL.add("ALTER TABLE employees ADD email VARCHAR2(100);");
        testSQL.add("ALTER TABLE employees ADD email LONG RAW;");
        testSQL.add("ALTER TABLE employees DROP COLUMN middle_name;");
        testSQL.add("ALTER TABLE employees MODIFY salary NUMBER(10,2);");
        testSQL.add("ALTER TABLE employees MODIFY salary ROWID;");
        testSQL.add("ALTER TABLE employees RENAME COLUMN first_name TO given_name;");
        testSQL.add("ALTER TABLE employees ADD CONSTRAINT emp_pk PRIMARY KEY (employee_id);");
        testSQL.add("ALTER TABLE employees DROP CONSTRAINT emp_pk;");
        testSQL.add("ALTER TABLE employees RENAME TO staff;");
        System.out.println("===== test of the alter table =====");
        System.out.println("The source DBMS is: " + CommonConfig.getSourceDB());
        System.out.println("The target DBMS is: " + CommonConfig.getTargetDB());
        System.out.println();
    }

    @Test
    public void testAlterTable()
    {
        int num = 1;
        for (String sql : testSQL) {
            System.out.println("===== test of the SQL" + num++ + " =====");
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
