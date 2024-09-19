package parser.ast.createTable;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class TableTypeNode extends ASTNode {
    public TableTypeNode(ASTNode node)
    {
        super(node);
    }
    public TableTypeNode(List<Token> tokens)
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

}
