package Parser.AST.IFELSIF;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class ELSIFConditionNode extends ASTNode {
    public ELSIFConditionNode() {
        super();
    }

    public ELSIFConditionNode(ASTNode node) {
        super(node);
    }

    public ELSIFConditionNode(List<Token> tokens) {
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

