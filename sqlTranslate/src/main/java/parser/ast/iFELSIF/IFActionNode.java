package parser.ast.iFELSIF;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class IFActionNode extends ASTNode {
    public IFActionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public IFActionNode(ASTNode node) {
        super(node);
    }

    public IFActionNode(List<Token> tokens) {
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
