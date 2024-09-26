package parser.ast.casewhen;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class CaseThenNode extends ASTNode {
    public CaseThenNode(ASTNode node) {
        super(node);
    }

    public CaseThenNode(List<Token> tokens) {
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
