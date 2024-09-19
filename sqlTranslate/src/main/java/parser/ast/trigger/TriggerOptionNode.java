package parser.ast.trigger;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerOptionNode extends ASTNode {
    public TriggerOptionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerOptionNode(ASTNode node) {
        super(node);
    }

    public TriggerOptionNode(List<Token> tokens) {
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
