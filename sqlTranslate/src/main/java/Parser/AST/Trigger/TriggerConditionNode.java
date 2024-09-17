package Parser.AST.Trigger;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerConditionNode extends ASTNode {
    private Token condition;
    private Token action;
    public TriggerConditionNode() {
        super();
    }

    public TriggerConditionNode(List<Token> tokens) {
        super(tokens);
    }

    public TriggerConditionNode(ASTNode node) {
        super(node);
    }

    public void setCondition(Token condition) {
        this.condition = condition;
    }

    public void setAction(Token action) {
        this.action = action;
    }

    public Token getCondition() {
        return condition;
    }

    public Token getAction() {
        return action;
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(getCondition().getValue() + getAction().getValue() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
