package parser.ast.trigger;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerObjNode extends ASTNode {
    public TriggerObjNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerObjNode(List<Token> tokens) {
        super(tokens);
    }

    public TriggerObjNode(ASTNode node) {
        super(node);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
