package parser.ast.pl;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class PLNode extends ASTNode {
    public PLNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public PLNode(ASTNode node) {
        super(node);
    }

    public PLNode(List<Token> tokens) {
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
