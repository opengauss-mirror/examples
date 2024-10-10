package parser.ast.function;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionBodyNode extends ASTNode {
    public FunctionBodyNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public FunctionBodyNode(ASTNode node) {
        super(node);
    }

    public FunctionBodyNode(List<Token> tokens) {
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
