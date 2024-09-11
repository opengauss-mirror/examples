package Lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OracleLexer {
    public static final String[] keywords = {"SELECT", "FROM", "WHERE", "AND", "OR", "INSERT", "UPDATE", "DELETE", "VALUES",
            // keywords of creating table
            "CREATE", "TABLE", "TEMPORARY", "GLOBAL",
            "NUMBER", "INTEGER", "SMALLIN", "BINARY_INTEGER", "DECIMAL", "REAL", "FLOAT", "DOUBLE PRECISION", "CHAR", "VARCHAR2",
            "NCHAR", "NVARCHAR2", "CLOB", "NCLOB", "DATE", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE", "BLOB", "RAW",
            "LONG RAW", "BOOLEAN", "INTERVAL YEAR TO MONTH", "INTERVAL DAY TO SECOND", "ROWID", "UROWID", "REF CURSOR",
            "NOT NULL", "PRIMARY KEY", "UNIQUE", "CHECK", "REFERENCES", "DEFAULT", "CONSTRAINT", "FOREIGN KEY"
            // keywords of drop table
            , "DROP", "CASCADE", "CONSTRAINTS"
            // keywords of select
            , "DISTINCT", "JOIN", "GROUP BY", "ORDER BY", "HAVING", "UNION", "CASE", "WHEN", "END", "AS"
            // keywords of join

    };
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(NUMBER\\(.*?\\))|" +                     // NUMBER() function
                    "(DECIMAL\\(.*?\\))|" +                   // DECIMAL() function
                    "(FLOAT\\(.*?\\))|" +                    // FLOAT() function
                    "(CHAR\\(.*?\\))|" +                     // CHAR() function
                    "(VARCHAR2\\(.*?\\))|" +                     // VARCHAR2() function
                    "(NCHAR\\(.*?\\))|" +                     // NCHAR() function
                    "(NVARCHAR2\\(.*?\\))|" +                     // NVARCHAR2() function
                    "(RAW\\(.*?\\))|" +                     // RAW() function

                    "(COUNT\\(.*?\\))|" +                     // COUNT() function
                    "(SUM\\(.*?\\))|" +                     // SUM() function
                    "(AVG\\(.*?\\))|" +                     // AVG() function
                    "(MAX\\(.*?\\))|" +                     // MAX() function
                    "(MIN\\(.*?\\))|" +                     // MIN() function

                    "(NOT NULL)|" +
                    "(PRIMARY KEY)|" +
                    "(FOREIGN KEY)|" +
                    "(DOUBLE PRECISION)|" +
                    "(TIMESTAMP WITH TIME ZONE)|" +
                    "(TIMESTAMP WITH LOCAL TIME ZONE)|" +
                    "(LONG RAW)|" +
                    "(INTERVAL YEAR TO MONTH)|" +
                    "(INTERVAL DAY TO SECOND)|" +
                    "(REF CURSOR)|" +
                    "(GROUP BY)|" +
                    "(ORDER BY)|" +


                    "(\\b[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*\\b)|" + // Keywords and identifiers
                    "(\\b[A-Za-z_][A-Za-z0-9_]*\\b)|" + // Keywords and identifiers
                    "(\\d+\\.?\\d*)|" +                 // Numbers (integer or decimal)
                    "(\"[^\"]*\")|" +                   // Double-quoted strings
                    "('([^']|\\\\')*)'|" +              // Single-quoted strings
                    "([;,()])|" +                     // Symbols: ;, (, ), ,
                    "([=<>+\\-*/<<>>])|" +              // Operators, including shift operators
                    "(\\s+)",                            // Whitespace
            Pattern.CASE_INSENSITIVE
    );

    private final List<Token> tokens = new ArrayList<>();
    private final String input;
    private int position = 0;

    public OracleLexer(String input) {
        this.input = dataProcess(input);
        tokenize();
    }

    public List<Token> tokenize() {
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        while (matcher.find()) {
            String tokenValue = matcher.group();
            Token token = createToken(tokenValue);
            if (token != null) { // Skip whitespace
                tokens.add(token);
                // Update position based on the length of the matched group
                position += tokenValue.length();
            } else {
                // If the token is null, we skip it (whitespace)
                position += tokenValue.length();
            }
        }
        tokens.add(new Token(Token.TokenType.EOF, ""));
        return tokens;
    }

    private Token createToken(String tokenValue) {
        if (tokenValue.matches("\\d+(\\.\\d+)?")) {
            return new Token(Token.TokenType.NUMBER, tokenValue);
        } else if (tokenValue.matches("\"[^\"]*\"")) {
            return new Token(Token.TokenType.STRING, tokenValue);
        } else if (tokenValue.matches("'([^']|\\\\')*'")) {
            return new Token(Token.TokenType.STRING, tokenValue);
        } else if (tokenValue.matches("([;(),])")) {
            return new Token(Token.TokenType.SYMBOL, tokenValue);
        } else if (tokenValue.matches("[=<>+\\-*/<<>>]")) {
            return new Token(Token.TokenType.OPERATOR, tokenValue);
        }

        else if (tokenValue.matches("(?i)NUMBER\\(.*?\\)")) {
            // NUMBER() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)DECIMAL\\(.*?\\)")) {
            // DECIMAL() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)FLOAT\\(.*?\\)")) {
            // FLOAT() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)CHAR\\(.*?\\)")) {
            // CHAR() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)VARCHAR2\\(.*?\\)")) {
            // VARCHAR2() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)NCHAR\\(.*?\\)")) {
            // NCHAR() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)NVARCHAR2\\(.*?\\)")) {
            // NVARCHAR2() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)RAW\\(.*?\\)")) {
            // RAW() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)COUNT\\(.*?\\)")) {
            // RAW() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)SUM\\(.*?\\)")) {
            // RAW() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)AVG\\(.*?\\)")) {
            // RAW() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)MAX\\(.*?\\)")) {
            // RAW() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }else if (tokenValue.matches("(?i)MIN\\(.*?\\)")) {
            // RAW() function, CASE_INSENSITIVE
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        }

        else if (isKeyword(tokenValue)) {
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        } else if (tokenValue.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")) {
            return new Token(Token.TokenType.IDENTIFIER, tokenValue);
        } else if (tokenValue.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return new Token(Token.TokenType.IDENTIFIER, tokenValue);
        }
        return null;
    }

    private boolean isKeyword(String tokenValue) {
        for (String keyword : keywords) {
            if (keyword.equalsIgnoreCase(tokenValue)) {
                return true;
            }
        }
        return false;
    }

    private static String dataProcess(String input){
        while (input.contains("  ")) {
            input = input.replace("  ", " ");
        }
        input = input.replace(" (", "(");
        return input;
    }

    public void printTokens() {
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
