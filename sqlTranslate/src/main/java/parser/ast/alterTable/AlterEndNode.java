package parser.ast.alterTable;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.List;

public class AlterEndNode extends ASTNode {
    public AlterEndNode(ASTNode node) {
        super(node);
    }

    public AlterEndNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}

