package parser.ast.procedure;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedureDeclareNode extends ASTNode {
    public ProcedureDeclareNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ProcedureDeclareNode(ASTNode node) {
        super(node);
    }

    public ProcedureDeclareNode(List<Token> tokens) {
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
