package parser.ast.caseWhen;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class CaseElseNode extends ASTNode {
    public CaseElseNode(ASTNode node) {
        super(node);
    }

    public CaseElseNode(List<Token> tokens) {
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
