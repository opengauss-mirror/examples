package Parser.AST.AlterTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class AlterAddColumnNode extends ASTNode {
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

    public Token getType() {
        return type;
    }

    public void setType(Token type) {
        this.type = type;
    }

    public List<Token> getConstraint() {
        return constraint;
    }

    public void setConstraint(List<Token> constraint) {
        this.constraint = constraint;
    }

    public void ResetTokensbyNameTypeConstraint() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(name);
        tokens.add(type);
        for (Token token : constraint) {
            tokens.add(token);
        }
        setTokens(tokens);
    }
}
