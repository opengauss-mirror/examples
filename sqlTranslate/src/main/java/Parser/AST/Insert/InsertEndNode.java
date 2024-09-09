package Parser.AST.Insert;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class InsertEndNode extends ASTNode {
    public InsertEndNode(ASTNode node) {
        super(node);
    }

    public InsertEndNode(List<Token> tokens) {
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
