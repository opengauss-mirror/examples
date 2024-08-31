package Parser.AST.Insert;

import Lexer.Token;
import Parser.AST.ASTNode;
import java.util.List;

public class InsertNode extends ASTNode {
    public InsertNode(List<Token> tokens)
    {
        super(tokens);
    }

    public InsertNode(ASTNode node)
    {
        super(node);
    }

    public void visit(ASTNode node, StringBuilder queryString)
    {
        queryString.append("INSERT INTO ");

    }
}
