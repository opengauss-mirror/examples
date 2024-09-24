import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import parser.ast.ASTNode;
import parser.OracleParser;


public class Main {
    public static void main(String[] args) {
        String sql = "SELECT e.first_name, d.department_name FROM employees e JOIN departments d Using e.department_id = d.department_id;";
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
