package Parser.AST.AlterTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class AlterAddConstraintNode extends ASTNode {
    public AlterAddConstraintNode(ASTNode node) {
        super(node);
    }

    public AlterAddConstraintNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())

            child.visit(child, queryString);
    }
}
