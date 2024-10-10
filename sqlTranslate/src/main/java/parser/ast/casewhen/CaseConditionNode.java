package parser.ast.casewhen;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class CaseConditionNode extends ASTNode {
    public CaseConditionNode(ASTNode node) {
        super(node);
    }

    public CaseConditionNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
