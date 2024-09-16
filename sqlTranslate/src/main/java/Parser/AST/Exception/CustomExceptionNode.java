package Parser.AST.Exception;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class CustomExceptionNode extends ASTNode {
    public CustomExceptionNode () {
        super();
        setTokens(new ArrayList<>());
    }

    public CustomExceptionNode (ASTNode node) {
        super(node);
        setTokens(new ArrayList<>());
    }

    public CustomExceptionNode (List<Token> tokens) {
        super(tokens);
        setTokens(new ArrayList<>());
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}
