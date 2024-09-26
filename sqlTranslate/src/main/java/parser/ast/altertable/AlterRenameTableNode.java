package parser.ast.altertable;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class AlterRenameTableNode extends ASTNode {
    public AlterRenameTableNode(ASTNode node) {
        super(node);
    }

    public AlterRenameTableNode(List<Token> tokens) {
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
