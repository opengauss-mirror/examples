package Interface;

import Lexer.Token;

public interface ColumnType {
    Token getType();
    void setType(Token type);
    void ResetTokensbyNameTypeConstraint();
}
