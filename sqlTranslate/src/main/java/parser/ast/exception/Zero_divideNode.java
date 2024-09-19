package parser.ast.exception;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class Zero_divideNode extends ASTNode {

    public Zero_divideNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public Zero_divideNode(ASTNode node) {
        super(node);
    }

    public Zero_divideNode(List<Token> tokens) {
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
