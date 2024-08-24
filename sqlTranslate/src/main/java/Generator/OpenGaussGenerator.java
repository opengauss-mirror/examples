package Generator;

import Lexer.Token;
import Parser.AST.ASTNode;
import Parser.AST.CreateTable.ColumnNode;
import Parser.AST.CreateTable.CreateTabNode;
import Exception.GenerateFailedException;

public class OpenGaussGenerator {
    private ASTNode node;
    public OpenGaussGenerator(ASTNode node) {
        this.node = node;
    }
    public String generate() {
        if (node instanceof CreateTabNode) {
            return GenCreatTableSQL(node);
        }
        else {
            throw new GenerateFailedException("Root node:" + node.getClass() + "(Unsupported node type!)");
        }
    }

    private String GenCreatTableSQL(ASTNode node) {
        // type convert
        visit(node);
        return node.toQueryString();
    }

    private void visit(ASTNode node) {
        if (node instanceof ColumnNode) {
            ColumnTypeConvert((ColumnNode) node);
        }
        for (ASTNode child : node.getChildren()) {
            visit(child);
        }
    }

    private void ColumnTypeConvert(ColumnNode node) {
        if (node.getType().getValue().equalsIgnoreCase("NUMBER")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "NUMERIC"));
            node.ResetTokensbyNameTypeConstraint();
        }
    }
}
