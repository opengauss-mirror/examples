package parser.ast.drop;

import lexer.Token;
import parser.ast.ASTNode;
import java.util.List;

public class DropEndNode extends ASTNode {
    public DropEndNode(ASTNode node) {
        super(node);
    }

    public DropEndNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
