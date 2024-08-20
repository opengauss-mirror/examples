package Lexer;

public class Token {
    public enum TokenType {
        KEYWORD, IDENTIFIER, NUMBER, OPERATOR, STRING, SYMBOL, EOF
    }

    private TokenType type;
    private final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean hasType(TokenType type) {
        return this.type == type;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", value='" + value + '\'' +
                '}';
    }

    public static Token createIdentifierToken(String value) {
        return new Token(TokenType.IDENTIFIER, value);
    }


    public static Token createKeywordToken(String value) {
        return new Token(TokenType.KEYWORD, value);
    }
}
