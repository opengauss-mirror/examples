package Parser.AST.IFELSIF;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ELSENode extends ASTNode {
    public ELSENode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ELSENode(ASTNode node) {
        super(node);
    }

    public ELSENode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}
