package Parser.AST.PL;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class PLBeginNode extends ASTNode {
    public PLBeginNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public PLBeginNode(ASTNode node) {
        super(node);
    }

    public PLBeginNode(List<Token> tokens) {
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
