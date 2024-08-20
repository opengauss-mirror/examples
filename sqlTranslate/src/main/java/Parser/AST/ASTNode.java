package Parser.AST;

import Lexer.Token;

import java.util.ArrayList;
import java.util.List;

public abstract class ASTNode {
    private List<Token> tokens;
    private ASTNode parent;
    private List<ASTNode> children = new ArrayList<>();

    public ASTNode(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ASTNode(ASTNode node) {
        this.tokens = node.tokens;
        this.parent = node.parent;
        this.children = new ArrayList<>(node.children);
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public ASTNode getParent() {
        return parent;
    }

    public void setParent(ASTNode parent) {
        this.parent = parent;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public ASTNode getChildByName(String childName) {
        for (ASTNode child : children) {
            if (child.toString().equals(childName)) {
                return child;
            }
        }

        return null;
    }

    public List<ASTNode> getChildrenByName(String childName) {
        List<ASTNode> nodes = new ArrayList<>();
        for (ASTNode child : children) {
            if (child.toString().equals(childName)) {
                nodes.add(child);
            }
        }

        return nodes;
    }

    /**
     * 将对应名称的node更新为新的child
     */
    public void setChildrenByName(String childName, ASTNode child) {
        for (int i = 0; i < this.children.size(); i++) {
            if (this.children.get(i).toString().contains(childName)) {
                this.children.set(i, child);
                break;
            }
        }
    }

    public void addChild(ASTNode childNode) {
        childNode.setParent(this);
        children.add(childNode);
    }

    public void replaceChild(ASTNode oldChild, ASTNode newChild) {
        newChild.setParent(this);
        children.set(children.indexOf(oldChild), newChild);
    }

    // add a child node at position index of children
    public void addChild(int index, ASTNode childNode) {
        childNode.setParent(this);
        children.add(index, childNode);
    }

    public void removeChild(ASTNode childNode) {
        children.remove(childNode);
    }

    public String toQueryString() {
        StringBuilder queryString = new StringBuilder();
        visit(this, queryString);
        return queryString.toString();
    }

    public abstract void visit(ASTNode node, StringBuilder queryString);

    @Override
    public String toString() {
        String str = "";
        for (int i = 0; i < tokens.size(); i++) {
            if (i < tokens.size() - 1)
                str += tokens.get(i).getValue() + " ";
            else
                str += tokens.get(i).getValue();
        }
        return str;
    }
}
