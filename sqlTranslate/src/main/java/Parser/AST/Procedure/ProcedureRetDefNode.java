package Parser.AST.Procedure;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedureRetDefNode extends ASTNode {
    public ProcedureRetDefNode() {
        super();
    }

    public ProcedureRetDefNode(ASTNode node) {
        super(node);
    }

    public ProcedureRetDefNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(") " + toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
