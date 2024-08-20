package Parser.AST;
import Lexer.Token;
import Parser.AST.ASTNode;
public class EndNode extends ASTNode{
    public EndNode(ASTNode node)
    {
        super(node);
    }
    public EndNode(Token token)
    {
        super(token);
    }
    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append(")" + getToken().getValue());
        for (ASTNode child : getChildren())
        {
            child.visit(child, queryString);
        }
    }
}
