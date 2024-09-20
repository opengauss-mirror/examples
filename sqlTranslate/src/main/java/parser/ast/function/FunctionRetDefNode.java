package parser.ast.function;

import interfaces.DataType;
import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionRetDefNode extends ASTNode implements DataType {
    private Token type;
    public FunctionRetDefNode() {
        super();
        setTokens(new ArrayList<>());
        setType(new Token(Token.TokenType.NULL, ""));
    }

    public FunctionRetDefNode(ASTNode node) {
        super(node);
    }

    public FunctionRetDefNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(") " + toString() + " ");
        for (ASTNode child : node.getChildren()) {
            child.visit(child, queryString);
        }
    }

    @Override
    public Token getType() {
        return type;
    }

    @Override
    public void setType(Token type) {
        this.type = type;
    }

    @Override
    public void ResetTokensbyNameTypeConstraint() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(new Token(Token.TokenType.KEYWORD, "RETURN"));
        tokens.add(type);
        tokens.add(new Token(Token.TokenType.KEYWORD, "IS"));
        setTokens(tokens);
    }
}
