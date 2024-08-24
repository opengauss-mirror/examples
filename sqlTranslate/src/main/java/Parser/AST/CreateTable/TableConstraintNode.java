package Parser.AST.CreateTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class TableConstraintNode extends ASTNode{
    public TableConstraintNode(ASTNode node)
    {
        super(node);
    }
    public TableConstraintNode(List<Token> tokens)
    {
        super(tokens);
    }

    public void visit(ASTNode node, StringBuilder queryString)
    {
        if (node.hasChild() && !(node.getChildren().get(0) instanceof CRTEndNode) )
            queryString.append(toString() + ", ");
        else if (node.hasChild() && (node.getChildren().get(0) instanceof CRTEndNode))
            queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
