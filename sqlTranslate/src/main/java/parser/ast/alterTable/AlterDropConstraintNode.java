package parser.ast.alterTable;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class AlterDropConstraintNode extends ASTNode {
    public AlterDropConstraintNode(ASTNode node) {
        super(node);
    }

    public AlterDropConstraintNode(List<Token> tokens) {
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
