package Parser.AST.Procedure;

import Interface.DataType;
import Lexer.Token;
import Parser.AST.ASTNode;

import java.util.ArrayList;
import java.util.List;

public class ProcedureColumnNode extends ASTNode implements DataType {
    private Token name;
    private Token type;
    private List<Token> constraint;
    private List<Token> InOut;
    public ProcedureColumnNode() {
        super();
        setTokens(new ArrayList<>());
        setInOut(new ArrayList<>());
    }

    public ProcedureColumnNode(ASTNode node)
    {
        super(node);
    }

    public ProcedureColumnNode(List<Token> tokens)
    {
        super(tokens);
    }

    @Override
    public void visit(ASTNode node, StringBuilder queryString)
    {
        if (node.hasChild() && (node.getChildren().get(0) instanceof ProcedureColumnNode) )
            queryString.append(toString() + ", ");
        else if (node.hasChild() && !(node.getChildren().get(0) instanceof ProcedureColumnNode))
            queryString.append(toString() + " ");
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }

    public Token getName() {
        return name;
    }

    public void setName(Token name) {
        this.name = name;
    }

    @Override
    public Token getType() {
        return type;
    }

    @Override
    public void setType(Token type) {
        this.type = type;
    }

    public List<Token> getConstraint() {
        return constraint;
    }

    public void setConstraint(List<Token> constraint) {
        this.constraint = constraint;
    }

    public List<Token> getInOut() {
        return InOut;
    }

    public void setInOut(List<Token> InOut) {
        this.InOut = InOut;
    }

    public void addInOut(Token InOut) {
        this.InOut.add(InOut);
    }

    @Override
    public void ResetTokensbyNameTypeConstraint() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(name);
        for (Token token : InOut) {
            tokens.add(token);
        }
        tokens.add(type);
        for (Token token : constraint) {
            tokens.add(token);
        }
        setTokens(tokens);
    }
}
