import Lexer.OracleLexer;
import Lexer.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String sql = "SELECT adef FROM table WHERE name = 'John Doe';";
        OracleLexer lexer = new OracleLexer(sql);
        List<Token> tokens = lexer.tokenize();

        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
