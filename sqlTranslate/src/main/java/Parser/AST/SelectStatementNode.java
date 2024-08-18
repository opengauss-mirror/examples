package Parser.AST;

import Lexer.Token;

public class SelectStatementNode extends ASTNode{
    public SelectStatementNode(ASTNode node)
    {
        super(node);
    }

    public SelectStatementNode(Token token)
    {
        super(token);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append("SELECT ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
