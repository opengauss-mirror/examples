package Parser.AST.Exception;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ExceptionNode extends ASTNode {

    public ExceptionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ExceptionNode(ASTNode node) {
        super(node);
    }

    public ExceptionNode(List<Token> tokens) {
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
