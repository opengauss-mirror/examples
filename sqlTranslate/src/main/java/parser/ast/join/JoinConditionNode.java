package parser.ast.join;

import lexer.Token;
import parser.ast.ASTNode;
import java.util.List;

public class JoinConditionNode extends ASTNode {
    private String keyword;
    public JoinConditionNode(List<Token> tokens)
    {
        super(tokens);
        this.keyword = "";
    }

    public JoinConditionNode(ASTNode node)
    {
        super(node);
        this.keyword = "";
    }

    public JoinConditionNode() {
        super();
    }

    public String getKeyword() {
        return keyword.replace(" ", "");
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword + " ";
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(this.keyword + toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
