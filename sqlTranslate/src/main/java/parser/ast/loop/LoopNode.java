package parser.ast.loop;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class LoopNode extends ASTNode {
    public LoopNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public LoopNode(ASTNode node) {
        super(node);
    }

    public LoopNode(List<Token> tokens) {
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
