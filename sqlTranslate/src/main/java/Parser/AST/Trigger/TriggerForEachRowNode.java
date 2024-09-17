package Parser.AST.Trigger;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerForEachRowNode extends ASTNode {
    public TriggerForEachRowNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerForEachRowNode(ASTNode node) {
        super(node);
    }

    public TriggerForEachRowNode(List<Token> tokens) {
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
