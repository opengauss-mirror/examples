package Parser.AST.Join;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class JoinTypeNode extends ASTNode {
    public JoinTypeNode(List<Token> tokens) {
        super(tokens);
    }

    public JoinTypeNode(ASTNode node) {
        super(node);
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
