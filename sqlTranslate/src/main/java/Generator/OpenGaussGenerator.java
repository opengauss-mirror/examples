package Generator;

import Parser.AST.ASTNode;

public class OpenGaussGenerator {
    private ASTNode node;
    public OpenGaussGenerator(ASTNode node) {
        this.node = node;
    }
    public String generate() {
        return node.toQueryString();
    }
}
