package parser.ast.procedure;

import lexer.Token;
import parser.ast.ASTNode;
import interfaces.DataType;

import java.util.ArrayList;
import java.util.List;

public class ProcedureRetDefNode extends ASTNode implements DataType {
    private Token type;
    public ProcedureRetDefNode() {
        super();
        setTokens(new ArrayList<>());
        setType(new Token(Token.TokenType.NULL, ""));
    }

    public ProcedureRetDefNode(ASTNode node) {
        super(node);
    }

    public ProcedureRetDefNode(List<Token> tokens) {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString) {
        queryString.append(") " + toString() + " ");
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
    public void ResetTokensbyType() {
        List <Token> tokens = new ArrayList<>();
        for (Token token: getTokens()) {
            if (token.hasType(Token.TokenType.KEYWORD) && !token.getValue().equalsIgnoreCase("IS")) {
                tokens.add(getType());
            }
            else {
                tokens.add(token);
            }
        }
        setTokens(tokens);
    }
}
