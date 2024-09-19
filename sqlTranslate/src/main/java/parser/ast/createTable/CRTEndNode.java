package parser.ast.createTable;
import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class CRTEndNode extends ASTNode {
    public CRTEndNode(ASTNode node)
    {
        super(node);
    }
    public CRTEndNode(List<Token> tokens)
    {
        super(tokens);
    }
    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(")" + toString());
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
