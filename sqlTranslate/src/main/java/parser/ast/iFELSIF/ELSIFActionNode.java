package parser.ast.iFELSIF;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ELSIFActionNode extends ASTNode {
    public ELSIFActionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ELSIFActionNode(ASTNode node) {
        super(node);
    }

    public ELSIFActionNode(List<Token> tokens) {
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
