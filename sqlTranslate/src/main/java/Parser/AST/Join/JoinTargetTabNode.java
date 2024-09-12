package Parser.AST.Join;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class JoinTargetTabNode extends ASTNode {
    public JoinTargetTabNode(List<Token> tokens) {
        super(tokens);
    }

    public JoinTargetTabNode(ASTNode node) {
        super(node);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
