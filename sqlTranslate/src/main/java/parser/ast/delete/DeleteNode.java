package parser.ast.delete;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class DeleteNode extends ASTNode {
    public DeleteNode(ASTNode node) {
        super(node);
    }
    public DeleteNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
