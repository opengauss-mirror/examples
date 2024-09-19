package parser.ast.delete;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class DeleteConditionNode extends ASTNode {
    public DeleteConditionNode(ASTNode node) {
        super(node);
    }

    public DeleteConditionNode(List<Token> tokens) {
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
