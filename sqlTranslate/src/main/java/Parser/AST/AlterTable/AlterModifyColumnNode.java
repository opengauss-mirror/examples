package Parser.AST.AlterTable;

import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.List;

public class AlterModifyColumnNode extends ASTNode {
    public AlterModifyColumnNode(ASTNode node) {
        super(node);
    }

    public AlterModifyColumnNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
        for (ASTNode child : getChildren())

            child.visit(child, queryString);
    }
}
