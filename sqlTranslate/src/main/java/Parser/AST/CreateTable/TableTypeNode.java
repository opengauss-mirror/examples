package Parser.AST.CreateTable;

import Lexer.Token;
import Parser.AST.ASTNode;

public class TableTypeNode extends ASTNode {
    public TableTypeNode(ASTNode node)
    {
        super(node);
    }
    public TableTypeNode(Token token)
    {
        super(token);
    }

    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(getToken() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }

}
