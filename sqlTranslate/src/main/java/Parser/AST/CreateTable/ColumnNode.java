package Parser.AST.CreateTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class ColumnNode extends ASTNode {
    private Token name;
    private Token type;
    private Token constraint;
    public ColumnNode() {
        super();
    }
    public ColumnNode(ASTNode node)
    {
        super(node);
    }
    public ColumnNode(List<Token> tokens)
    {
        super(tokens);
    }

    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
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

    public Token getConstraint() {
        return constraint;
    }

    public void setConstraint(Token constraint) {
        this.constraint = constraint;
    }

}
