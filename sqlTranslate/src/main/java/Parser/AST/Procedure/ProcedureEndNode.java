package Parser.AST.Procedure;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedureEndNode extends ASTNode {
    public ProcedureEndNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ProcedureEndNode(ASTNode node) {
        super(node);
    }

    public ProcedureEndNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
