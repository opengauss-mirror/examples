package interfaces;

import lexer.Token;

public interface DataType {
    Token getType();
    void setType(Token type);
    void ResetTokensbyType();
}
