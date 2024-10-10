package parser.ast.exception;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class OtherExceptionNode extends ASTNode {

    public OtherExceptionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public OtherExceptionNode(ASTNode node) {
        super(node);
    }

    public OtherExceptionNode(List<Token> tokens) {
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
