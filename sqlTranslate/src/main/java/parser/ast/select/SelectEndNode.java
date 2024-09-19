package parser.ast.select;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class SelectEndNode extends ASTNode {
    public SelectEndNode(List<Token> tokens)
    {
        super(tokens);
    }

    public SelectEndNode(ASTNode node)
    {
        super(node);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(toString());
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
