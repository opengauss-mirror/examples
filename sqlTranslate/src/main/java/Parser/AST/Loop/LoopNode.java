package Parser.AST.Loop;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class LoopNode extends ASTNode {
    public LoopNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public LoopNode(ASTNode node) {
        super(node);
        setTokens(new ArrayList<>());
    }

    public LoopNode(List<Token> tokens) {
        super(tokens);
        setTokens(new ArrayList<>());
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
