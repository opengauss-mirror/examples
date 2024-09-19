package parser.ast.join;

import lexer.Token;
import parser.ast.ASTNode;
import java.util.List;

public class JoinSourceTabNode extends ASTNode {
    public JoinSourceTabNode(List<Token> tokens) {
        super(tokens);
    }

    public JoinSourceTabNode(ASTNode node) {
        super(node);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
