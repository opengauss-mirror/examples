import Lexer.OracleLexer;
import Lexer.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
//        String sql = "SELECT NuMBER(1,3) Not NULL FROM table WHERE name = 'John Doe';";
        String sql = "CREATE TABLE employees (\n" +
                "    employee_id NUMBER(6) PRIMARY KEY,\n" +
                "    first_name VARCHAR2(20),\n" +
                "    last_name VARCHAR2(25),\n" +
                "    email VARCHAR2(25),\n" +
                "    hire_date DATE\n" +
                ");";
        OracleLexer lexer = new OracleLexer(sql);
        lexer.printTokens();
//        List<Token> tokens = lexer.tokenize();
//
//        for (Token token : tokens) {
//            System.out.println(token);
//        }
    }
}
