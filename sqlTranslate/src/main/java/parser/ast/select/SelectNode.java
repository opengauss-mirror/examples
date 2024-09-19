package parser.ast.select;

import parser.ast.ASTNode;
import lexer.Token;

import java.util.List;

public class SelectNode extends ASTNode {
    public SelectNode(List<Token> tokens)
    {
        super(tokens);
    }

    public SelectNode(ASTNode node)
    {
        super(node);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }

}
