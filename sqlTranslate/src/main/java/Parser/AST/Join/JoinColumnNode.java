package Parser.AST.Join;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class JoinColumnNode extends ASTNode {
    private String keyword;
    public JoinColumnNode(List<Token> tokens)
    {
        super(tokens);
        this.keyword = "";
    }

    public JoinColumnNode(ASTNode node)
    {
        super(node);
        this.keyword = "";
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
