package Parser.AST.View;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class ViewEndNode extends ASTNode {
    public ViewEndNode() {
        super();
    }

    public ViewEndNode(ASTNode node) {
        super(node);
    }

    public ViewEndNode(List<Token> tokens) {
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
