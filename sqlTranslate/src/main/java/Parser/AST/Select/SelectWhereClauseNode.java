package Parser.AST.Select;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class SelectWhereClauseNode extends ASTNode {
    public SelectWhereClauseNode(List<Token> tokens)
    {
        super(tokens);
    }

    public SelectWhereClauseNode(ASTNode node)
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
