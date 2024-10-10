package parser.ast.select;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class SelectObjNode extends ASTNode {
    public SelectObjNode(List<Token> tokens)
    {
        super(tokens);
    }

    public SelectObjNode(ASTNode node)
    {
        super(node);
    }

    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
