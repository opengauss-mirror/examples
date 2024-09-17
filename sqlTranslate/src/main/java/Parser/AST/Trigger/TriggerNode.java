package Parser.AST.Trigger;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerNode extends ASTNode {
    public TriggerNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerNode(ASTNode node) {
        super(node);
    }

    public TriggerNode(List<Token> tokens) {
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
