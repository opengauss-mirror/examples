package Parser.AST.IFELSIF;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ELSIFConditionNode extends ASTNode {
    public ELSIFConditionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ELSIFConditionNode(ASTNode node) {
        super(node);
        setTokens(new ArrayList<>());
    }

    public ELSIFConditionNode(List<Token> tokens) {
        super(tokens);
        setTokens(new ArrayList<>());
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }
}

