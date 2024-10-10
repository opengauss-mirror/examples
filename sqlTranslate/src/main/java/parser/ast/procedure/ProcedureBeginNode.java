package parser.ast.procedure;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedureBeginNode extends ASTNode {
    public ProcedureBeginNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ProcedureBeginNode(ASTNode node) {
        super(node);
    }

    public ProcedureBeginNode(List<Token> tokens) {
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
