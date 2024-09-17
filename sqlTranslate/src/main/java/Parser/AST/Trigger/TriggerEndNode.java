package Parser.AST.Trigger;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerEndNode extends ASTNode {
    public TriggerEndNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerEndNode(ASTNode node) {
        super(node);
    }

    public TriggerEndNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
