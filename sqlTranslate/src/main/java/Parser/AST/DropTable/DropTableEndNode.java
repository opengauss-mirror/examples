package Parser.AST.DropTable;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class DropTableEndNode extends ASTNode {
    public DropTableEndNode(ASTNode node) {
        super(node);
    }

    public DropTableEndNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString());
        for (ASTNode child : getChildren()) {
            child.visit(child, queryString);
        }
    }
}
