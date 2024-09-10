package Parser.AST.Select;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class SelectObjNode extends ASTNode {
    public SelectObjNode(List<Token> tokens)
    {
        super(tokens);
        this.isDistinct = "";
    }

    public SelectObjNode(ASTNode node)
    {
        super(node);
        this.isDistinct = "";
    }

    private String isDistinct;

    public SelectObjNode() {
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
    public String toString()
    {
        List<Token> tokens = getTokens();
        String str = "";
        for (int i = 0; i < tokens.size(); i++) {
            if (i < tokens.size() - 1)
                str += tokens.get(i).getValue() + ", ";
            else
                str += tokens.get(i).getValue();
        }
        return str;
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
