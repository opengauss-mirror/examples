
import generator.OpenGaussGenerator;
import lexer.OracleLexer;
import parser.ast.ASTNode;
import parser.OracleParser;


public class Main {
    public static void main(String[] args) {
        String sql = "SELECT e.first_name, d.department_name FROM employees e JOIN departments d Using e.department_id = d.department_id;";
//        String sql = "CASE WHEN column2 > 0 THEN 'Positive' ELSE 'Non-positive' END";
//
        OracleLexer lexer = new OracleLexer(sql);
        lexer.printTokens();
        OracleParser parser = new OracleParser(lexer);
        ASTNode root = parser.parse();
//        ASTNode root = OracleParser.parseException(lexer.getTokens());
//        ASTNode root = OracleParser.parseLoop(lexer.getTokens());
//        ASTNode root = OracleParser.parseIFELSE(lexer.getTokens());
//        ASTNode root = OracleParser.parseCaseWhen(lexer.getTokens());
//        ASTNode root = OracleParser.parseJoin(lexer.getTokens());
        System.out.println(root.toQueryString());
        System.out.println(root.getASTString());
        OpenGaussGenerator generator = new OpenGaussGenerator(root);
        System.out.println(generator.generate());
    }
}
