package parser.ast.view;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class ViewTargetNode extends ASTNode {
    public ViewTargetNode(ASTNode node) {
        super(node);
    }

    public ViewTargetNode(List<Token> tokens) {
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
