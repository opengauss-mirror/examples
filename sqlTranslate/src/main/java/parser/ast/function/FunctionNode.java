package parser.ast.function;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionNode extends ASTNode {
    public FunctionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public FunctionNode(ASTNode node) {
        super(node);
    }

    public FunctionNode(List<Token> tokens) {
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
