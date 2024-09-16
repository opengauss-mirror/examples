package Parser.AST.Exception;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ExceptionActionNode extends ASTNode {
    public ExceptionActionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ExceptionActionNode(ASTNode node) {
        super(node);
    }

    public ExceptionActionNode(List<Token> tokens) {
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
