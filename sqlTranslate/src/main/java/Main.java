
import Generator.OpenGaussGenerator;
import Lexer.OracleLexer;
import Parser.AST.ASTNode;
import Parser.OracleParser;


public class Main {
    public static void main(String[] args) {
        String sql = "CREATE TABLE employees (\n" +
                "    employee_id NUMBER PRIMARY KEY,\n" +
                "    first_name VARCHAR2(20),\n" +
                "    last_name VARCHAR2(25),\n" +
                "    email VARCHAR2(25),\n" +
                "    hire_date DATE\n" +
                ");";
        OracleLexer lexer = new OracleLexer(sql);
        lexer.printTokens();
        OracleParser parser = new OracleParser(lexer);
        ASTNode root = parser.parse();
        System.out.println(root.toQueryString());
        System.out.println(root.getASTString());
        OpenGaussGenerator generator = new OpenGaussGenerator(root);
        System.out.println(generator.generate());
    }
}
