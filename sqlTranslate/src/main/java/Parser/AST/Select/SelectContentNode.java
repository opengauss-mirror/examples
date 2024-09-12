package Parser.AST.Select;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class SelectContentNode extends ASTNode {
    public SelectContentNode(List<Token> tokens)
    {
        super(tokens);
        this.isDistinct = "";
    }

    public SelectContentNode(ASTNode node)
    {
        super(node);
        this.isDistinct = "";
    }

    private String isDistinct;

    public SelectContentNode() {
        super();
    }

    public String getIsDistinct()
    {
        return isDistinct;
    }

    public void setIsDistinct(String isDistinct) {
        this.isDistinct = isDistinct + " ";
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(isDistinct + toString() + " FROM ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
