package parser.ast.trigger;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerBodyNode extends ASTNode {
    public TriggerBodyNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerBodyNode(ASTNode node) {
        super(node);
    }

    public TriggerBodyNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
