package Parser.AST.Procedure;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedurePLStatementNode extends ASTNode {
    public ProcedurePLStatementNode() {
        super();
        setTokens(new ArrayList<>());
    }
    public ProcedurePLStatementNode(ASTNode node) {
        super(node);
    }
    public ProcedurePLStatementNode(List<Token> tokens) {
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
