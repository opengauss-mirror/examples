package parser.ast.exception;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class Invalid_numberNode extends ASTNode {

    public Invalid_numberNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public Invalid_numberNode(ASTNode node) {
        super(node);
    }

    public Invalid_numberNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}
