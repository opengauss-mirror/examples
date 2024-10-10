package parser.ast.altertable;

import interfaces.DataType;
import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class AlterModifyColumnNode extends ASTNode implements DataType {
    private Token name;
    private Token type;

    public AlterModifyColumnNode() {
        super();
    }

    public AlterModifyColumnNode(ASTNode node) {
        super(node);
    }

    public AlterModifyColumnNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void setType(Token columnType) {
        this.type = columnType;
    }

    @Override
    public Token getType() {
        return type;
    }

    public void setName(Token columnName) {
        this.name = columnName;
    }

    public Token getName() {
        return name;
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }

    @Override
    public void ResetTokensbyType() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(new Token(Token.TokenType.KEYWORD, "MODIFY"));
        tokens.add(name);
        tokens.add(type);
        setTokens(tokens);
    }
}
