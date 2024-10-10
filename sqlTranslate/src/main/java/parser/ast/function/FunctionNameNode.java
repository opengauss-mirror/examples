package parser.ast.function;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionNameNode extends ASTNode {
    public FunctionNameNode(List<Token> tokens) {
        super(tokens);
    }

    public FunctionNameNode(ASTNode node) {
        super(node);
    }

    public FunctionNameNode() {
        super();
        setTokens(new ArrayList<>());
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ( ");
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}
