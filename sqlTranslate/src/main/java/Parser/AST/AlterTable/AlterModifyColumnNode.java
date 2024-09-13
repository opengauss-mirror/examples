package Parser.AST.AlterTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class AlterModifyColumnNode extends ASTNode {
    private Token columnName;
    private Token columnType;

    public AlterModifyColumnNode() {
        super();
    }

    public AlterModifyColumnNode(ASTNode node) {
        super(node);
    }

    public AlterModifyColumnNode(List<Token> tokens) {
        super(tokens);
    }

    public void setColumnType(Token columnType) {
        this.columnType = columnType;
    }

    public Token getColumnType() {
        return columnType;
    }

    public void setColumnName(Token columnName) {
        this.columnName = columnName;
    }

    public Token getColumnName() {
        return columnName;
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }

    public void ResetTokensbyNameTypeConstraint() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(new Token(Token.TokenType.KEYWORD, "MODIFY"));
        tokens.add(columnName);
        tokens.add(columnType);
        tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
        setTokens(tokens);
    }
}
