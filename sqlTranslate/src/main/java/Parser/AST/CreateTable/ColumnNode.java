package Parser.AST.CreateTable;

import Lexer.Token;
import Parser.AST.ASTNode;

public class ColumnNode extends ASTNode {
    public ColumnNode(ASTNode node)
    {
        super(node);
    }
    public ColumnNode(Token token)
    {
        super(token);
    }

    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(getToken().getValue() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }

}
