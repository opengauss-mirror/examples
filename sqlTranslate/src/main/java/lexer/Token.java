package lexer;

import java.util.Objects;

public class Token {
    public enum TokenType {
        KEYWORD, IDENTIFIER, NUMBER, OPERATOR, STRING, SYMBOL, NULL, EOF
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token token = (Token) obj;
        return type == token.type && Objects.equals(value, token.value);
    }

}
