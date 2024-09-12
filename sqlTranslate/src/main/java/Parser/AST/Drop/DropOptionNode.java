package Parser.AST.Drop;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class DropOptionNode extends ASTNode {
    public DropOptionNode(ASTNode node) {
        super(node);
    }

    public DropOptionNode(List<Token> tokens) {
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
