package Parser.AST.IFELSIF;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class IFConditionNode extends ASTNode {
    public IFConditionNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public IFConditionNode(ASTNode node) {
        super(node);
        setTokens(new ArrayList<>());
    }

    public IFConditionNode(List<Token> tokens) {
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
