package Parser.AST.CreateTable;

import Lexer.Token;
import Parser.AST.ASTNode;

public class CreateTabNode extends ASTNode {
    public CreateTabNode(ASTNode node) {
        super(node);
    }

    public CreateTabNode(Token token) {
        super(token);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        // judge the type of the table
        if (getChildren().get(0) instanceof TableTypeNode) {
            queryString.append("CREATE ");
        }
        else {
            queryString.append("CREATE TABLE ");
        }
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
