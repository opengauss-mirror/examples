package parser.ast.alterTable;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class AlterNode extends ASTNode {
    public AlterNode(ASTNode node) {
        super(node);
    }

    public AlterNode(List<Token> tokens) {
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
