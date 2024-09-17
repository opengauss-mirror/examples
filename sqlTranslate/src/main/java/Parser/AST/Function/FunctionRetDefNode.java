package Parser.AST.Function;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionRetDefNode extends ASTNode {
    public FunctionRetDefNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public FunctionRetDefNode(ASTNode node) {
        super(node);
    }

    public FunctionRetDefNode(List<Token> tokens) {
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
