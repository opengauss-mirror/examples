package Parser.AST.Insert;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class InsertDataNode extends ASTNode {
    public InsertDataNode(List<Token> tokens) {
        super(tokens);
    }

    public InsertDataNode(ASTNode node) {
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
