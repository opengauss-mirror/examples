package Parser.AST.Loop;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class WhileNode extends ASTNode {
    public WhileNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public WhileNode(ASTNode node) {
        super(node);
    }

    public WhileNode(List<Token> tokens) {
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
