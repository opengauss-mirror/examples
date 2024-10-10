package parser.ast.procedure;

import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedureObjNode extends ASTNode {
    public ProcedureObjNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ProcedureObjNode(ASTNode node) {
        super(node);
    }

    public ProcedureObjNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " (");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }

}
