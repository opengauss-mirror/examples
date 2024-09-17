package Parser.AST.Trigger;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerBeginNode extends ASTNode {
    public TriggerBeginNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public TriggerBeginNode(ASTNode node) {
        super(node);
    }

    public TriggerBeginNode(List<Token> tokens) {
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
