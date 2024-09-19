package parser.ast.alterTable;

import interfaces.DataType;
import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class AlterAddColumnNode extends ASTNode implements DataType {
    private Token name;
    private Token type;
    private List<Token> constraint;

    public AlterAddColumnNode() {
        super();
    }

    public AlterAddColumnNode(ASTNode node)
    {
        super(node);
    }

    public AlterAddColumnNode(List<Token> tokens)
    {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }

    public Token getName() {
        return name;
    }

    public void setName(Token name) {
        this.name = name;
    }

    @Override
    public Token getType() {
        return type;
    }

    @Override
    public void setType(Token type) {
        this.type = type;
    }

    public List<Token> getConstraint() {
        return constraint;
    }

    public void setConstraint(List<Token> constraint) {
        this.constraint = constraint;
    }

    @Override
    public void ResetTokensbyNameTypeConstraint() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(new Token(Token.TokenType.KEYWORD, "ADD"));
        tokens.add(name);
        tokens.add(type);
        for (Token token : constraint) {
            tokens.add(token);
        }
        setTokens(tokens);
    }
}
