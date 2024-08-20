package Parser.AST.CreateTable;

import Lexer.Token;
import Parser.AST.ASTNode;

public class TableNode extends ASTNode{
    public TableNode(ASTNode node)
    {
        super(node);
    }
    public TableNode(Token token)
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
