package parser.ast.update;

import parser.ast.ASTNode;
import lexer.Token;

import java.util.List;

public class UpdateEndNode extends ASTNode {
    public UpdateEndNode(ASTNode node) {
        super(node);
    }

    public UpdateEndNode(List<Token> tokens) {
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
