package Parser.AST.DropTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class DropTableOptionNode extends ASTNode {
    public DropTableOptionNode(ASTNode node) {
        super(node);
    }

    public DropTableOptionNode(List<Token> tokens) {
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
