package parser.ast.function;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionEndNode extends ASTNode {
    public FunctionEndNode(List<Token> tokens) {
        super(tokens);
    }

    public FunctionEndNode(ASTNode node) {
        super(node);
    }

    public FunctionEndNode() {
        super();
        setTokens(new ArrayList<>());
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}
