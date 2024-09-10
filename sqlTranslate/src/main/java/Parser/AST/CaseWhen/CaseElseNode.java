package Parser.AST.CaseWhen;

import Lexer.Token;
import Parser.AST.ASTNode;

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
        queryString.append("ELSE " + toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
