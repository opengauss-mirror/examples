package Parser.AST.IFELSIF;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class EndIFNode extends ASTNode {
    public EndIFNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public EndIFNode (ASTNode node) {
        super(node);
        setTokens(new ArrayList<>());
    }

    public EndIFNode (List<Token> tokens) {
        super(tokens);
        setTokens(new ArrayList<>());
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}
