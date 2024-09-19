package parser.ast.insert;

import lexer.Token;
import parser.ast.ASTNode;
import java.util.List;

public class InsertObjNode extends ASTNode{
    public InsertObjNode(List<Token> tokens) {
        super(tokens);
    }

    public InsertObjNode(ASTNode node) {
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
