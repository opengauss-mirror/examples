package Parser.AST.Select;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class SelectUnionNode extends ASTNode {
    public SelectUnionNode(List<Token> tokens)
    {
        super(tokens);
    }

    public SelectUnionNode(ASTNode node)
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
