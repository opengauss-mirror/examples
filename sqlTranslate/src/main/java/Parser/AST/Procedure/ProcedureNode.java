package Parser.AST.Procedure;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedureNode extends ASTNode {
    public ProcedureNode() {
        super();
        setTokens(new ArrayList<>());
    }

    public ProcedureNode(ASTNode node) {
        super(node);
    }

    public ProcedureNode(List<Token> tokens) {
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
