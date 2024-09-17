package Parser.AST.Trigger;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerNameNode extends ASTNode {
    public TriggerNameNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerNameNode(ASTNode node) {
        super(node);
    }

    public TriggerNameNode(List<Token> tokens) {
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
