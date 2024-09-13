package Parser.AST.IFELSIF;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class ElSIFActionNode extends ASTNode {
    public ElSIFActionNode(ASTNode node) {
        super(node);
    }

    public ElSIFActionNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}
