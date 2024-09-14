package Parser.AST.IFELSIF;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class IFActionNode extends ASTNode {
    public IFActionNode() {
        super();
    }

    public IFActionNode(ASTNode node) {
        super(node);
    }

    public IFActionNode(List<Token> tokens) {
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
