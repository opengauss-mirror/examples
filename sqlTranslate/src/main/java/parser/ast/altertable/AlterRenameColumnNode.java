package parser.ast.altertable;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class AlterRenameColumnNode extends ASTNode {
    public AlterRenameColumnNode(ASTNode node) {
        super(node);
    }

    public AlterRenameColumnNode(List<Token> tokens) {
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
