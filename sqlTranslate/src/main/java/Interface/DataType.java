package Interface;

import Lexer.Token;

public interface DataType {
    Token getType();
    void setType(Token type);
    void ResetTokensbyNameTypeConstraint();
}
