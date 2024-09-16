package Parser.AST.Exception;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class Zero_divideNode extends ASTNode {

    public Zero_divideNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public Zero_divideNode(ASTNode node) {
        super(node);
        setTokens(new ArrayList<>());
    }

    public Zero_divideNode(List<Token> tokens) {
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
