package Parser.AST.CreateTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class ColumnNode extends ASTNode {
    public ColumnNode(ASTNode node)
    {
        super(node);
    }
    public ColumnNode(List<Token> tokens)
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
