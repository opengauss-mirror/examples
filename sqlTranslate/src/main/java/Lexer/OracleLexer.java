package Lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OracleLexer {
    public static final String[] keywords = {"SELECT", "FROM", "WHERE", "AND", "OR", "INSERT", "UPDATE", "DELETE", "now()"};
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(now\\(\\))|" +                    // now() function
            "(\\b[A-Za-z_][A-Za-z0-9_]*\\b)|" + // Keywords and identifiers
                    "(\\d+\\.?\\d*)|" +                 // Numbers (integer or decimal)
                    "(\"[^\"]*\")|" +                   // Double-quoted strings
                    "('([^']|\\\\')*)'|" +              // Single-quoted strings
                    "([;,()])|" +                     // Symbols: ;, (, ), ,
                    "([=<>+\\-*/<<>>])|" +              // Operators, including shift operators
                    "(\\s+)"                            // Whitespace
    );

    private final List<Token> tokens = new ArrayList<>();
    private final String input;
    private int position = 0;

    public OracleLexer(String input) {
        this.input = input;
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
        } else if (tokenValue.matches("now\\(\\)")) {
            // Handle the now() function as a single token
            return new Token(Token.TokenType.KEYWORD, tokenValue);
        } else if (isKeyword(tokenValue)) {
            return new Token(Token.TokenType.KEYWORD, tokenValue);
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
}
