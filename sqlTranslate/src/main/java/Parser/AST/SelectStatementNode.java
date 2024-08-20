package Parser.AST;

import Lexer.Token;

import java.util.List;

public class SelectStatementNode extends ASTNode{
    public SelectStatementNode(ASTNode node)
    {
        super(node);
    }

    public SelectStatementNode(List<Token> tokens)
    {
        super(tokens);
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
