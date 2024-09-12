package Parser.AST.Update;

import Parser.AST.ASTNode;
import Lexer.Token;

import java.util.List;

public class UpdateWhereNode extends ASTNode {
    public UpdateWhereNode(ASTNode node) {
        super(node);
    }

    public UpdateWhereNode(List<Token> tokens) {
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
