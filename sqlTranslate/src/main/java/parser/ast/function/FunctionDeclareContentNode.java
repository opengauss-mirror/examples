package parser.ast.function;

import interfaces.DataType;
import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionDeclareContentNode extends ASTNode implements DataType {
    private Token type;
    public FunctionDeclareContentNode() {
        super();
        setTokens(new ArrayList<>());
        setType(new Token(Token.TokenType.NULL, ""));
    }

    public FunctionDeclareContentNode(List<Token> tokens) {
        super(tokens);
    }

    public FunctionDeclareContentNode(ASTNode node) {
        super(node);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(toString() + " ");
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
    public void ResetTokensbyType() {
        List<Token> tokens = new ArrayList<>();
        for (Token token : getTokens()) {
            if (token.hasType(Token.TokenType.KEYWORD) && !token.getValue().equalsIgnoreCase(":=")) {
                tokens.add(getType());
            }
            else {
                tokens.add(token);
            }
        }
        setTokens(tokens);
    }
}
