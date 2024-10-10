package parser.ast.pl;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class PLEndNode extends ASTNode {
    public PLEndNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public PLEndNode(ASTNode node) {
        super(node);
    }

    public PLEndNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
