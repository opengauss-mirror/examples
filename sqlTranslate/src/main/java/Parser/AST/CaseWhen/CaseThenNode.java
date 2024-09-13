package Parser.AST.CaseWhen;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class CaseThenNode extends ASTNode {
    public CaseThenNode(ASTNode node) {
        super(node);
    }

    public CaseThenNode(List<Token> tokens) {
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
