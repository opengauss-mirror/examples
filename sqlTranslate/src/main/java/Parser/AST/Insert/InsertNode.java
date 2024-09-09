package Parser.AST.Insert;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class InsertNode extends ASTNode {
    public InsertNode(List<Token> tokens)
    {
        super(tokens);
    }

    public InsertNode(ASTNode node)
    {
        super(node);
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
