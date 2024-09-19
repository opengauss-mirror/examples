package parser.ast.pl;

import interfaces.DataType;
import lexer.Token;
import parser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class PLDeclareNode extends ASTNode implements DataType {
    private Token type;
    public PLDeclareNode() {
        super();
        setTokens(new ArrayList<>());
        setType(new Token(Token.TokenType.NULL, ""));
    }

    public PLDeclareNode(ASTNode node) {
        super(node);
    }

    public PLDeclareNode(List<Token> tokens) {
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
        for (Token token: getTokens()) {
            if (token.hasType(Token.TokenType.KEYWORD)) {
                tokens.add(getType());
            }
            else {
                tokens.add(token);
            }
        }
        setTokens(tokens);
    }
}
