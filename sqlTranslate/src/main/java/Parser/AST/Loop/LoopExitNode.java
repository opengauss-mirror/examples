package Parser.AST.Loop;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class LoopExitNode extends ASTNode {
    public LoopExitNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public LoopExitNode(ASTNode node) {
        super(node);
    }

    public LoopExitNode(List<Token> tokens) {
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
