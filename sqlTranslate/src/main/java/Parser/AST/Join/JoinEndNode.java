package Parser.AST.Join;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class JoinEndNode extends ASTNode {
    public JoinEndNode(List<Token> tokens) {
        super(tokens);
    }

    public JoinEndNode(ASTNode node) {
        super(node);
    }

    public JoinEndNode() {
        super();
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
