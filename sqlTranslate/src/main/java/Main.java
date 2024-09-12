
import Generator.OpenGaussGenerator;
import Lexer.OracleLexer;
import Parser.AST.ASTNode;
import Parser.OracleParser;


public class Main {
    public static void main(String[] args) {
//        String sql = "CREATE TABLE employees (\n" +
//                "    employee_id NUMBER PRIMARY KEY,\n" +
//                "    first_name VARCHAR2(20),\n" +
//                "    last_name VARCHAR2(25),\n" +
//                "    email VARCHAR2(25),\n" +
//                "    hire_date DATE\n" +
//                ");";
//        String sql = "INSERT INTO employees (first_name, last_name, email) VALUES ('Jane', 'Smith', 'janesm@example.com');";
//        String sql = "DROP TABLE employees CASCADE CONSTRAINTS;";
        String sql = "SELECT e.first_name, d.department_name FROM employees e JOIN departments d ON e.department_id = d.department_id;";
//        String sql = "Customers\n" +
//                "LEFT JOIN Orders ON Customers.CustomerID = Orders.CustomerID where";
        OracleLexer lexer = new OracleLexer(sql);
        lexer.printTokens();
        OracleParser parser = new OracleParser(lexer);
        ASTNode root = parser.parse();
//        ASTNode root = OracleParser.parseJoin(lexer.getTokens());
        System.out.println(root.toQueryString());
        System.out.println(root.getASTString());
        OpenGaussGenerator generator = new OpenGaussGenerator(root);
        System.out.println(generator.generate());
    }
}
