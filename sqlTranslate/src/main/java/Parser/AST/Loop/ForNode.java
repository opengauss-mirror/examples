package Parser.AST.Loop;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ForNode extends ASTNode {
    public ForNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ForNode(ASTNode node) {
        super(node);
    }

    public ForNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
