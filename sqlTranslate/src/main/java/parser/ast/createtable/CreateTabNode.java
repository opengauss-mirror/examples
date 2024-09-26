package parser.ast.createtable;

import lexer.Token;
import parser.ast.ASTNode;
import java.util.List;

public class CreateTabNode extends ASTNode {
    public CreateTabNode(ASTNode node) {
        super(node);
    }
    public CreateTabNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        // judge the type of the table
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
