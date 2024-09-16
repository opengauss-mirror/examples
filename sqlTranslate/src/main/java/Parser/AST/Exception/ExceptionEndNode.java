package Parser.AST.Exception;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ExceptionEndNode extends ASTNode {
    public ExceptionEndNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ExceptionEndNode(ASTNode node) {
        super(node);
    }

    public ExceptionEndNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {

    }
}
