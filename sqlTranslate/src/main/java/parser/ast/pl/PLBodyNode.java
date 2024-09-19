package parser.ast.pl;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class PLBodyNode extends ASTNode {
    public PLBodyNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public PLBodyNode(ASTNode node) {
        super(node);
    }

    public PLBodyNode(List<Token> tokens) {
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
