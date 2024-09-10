package Parser.AST.Select;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class SelectTableNode extends ASTNode {
    public SelectTableNode(List<Token> tokens)
    {
        super(tokens);
    }

    public SelectTableNode(ASTNode node)
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
