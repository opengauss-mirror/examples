package Parser.AST.Function;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionReturnNode extends ASTNode {
    public FunctionReturnNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public FunctionReturnNode(ASTNode node) {
        super(node);
    }

    public FunctionReturnNode(List<Token> tokens) {
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
