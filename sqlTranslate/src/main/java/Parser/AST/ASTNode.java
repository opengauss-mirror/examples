package Parser.AST;

import Lexer.Token;

import java.util.ArrayList;
import java.util.List;

public abstract class ASTNode {
    private List<Token> tokens;
    private ASTNode parent;
    private List<ASTNode> children = new ArrayList<>();

    public ASTNode() {
    }

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

    public boolean tokensEqual(List<Token> tokens) {
        if (this.tokens.size() != tokens.size())
            return false;

        for (int i = 0; i < tokens.size(); i++) {
            if (!this.tokens.get(i).getValue().equals(tokens.get(i).getValue()))
                return false;
        }

        return true;
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

    public boolean hasChild() {
        return !children.isEmpty();
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

    public ASTNode getDeepestChild() {
        if (hasChild()) {
            ASTNode child = getChildren().get(0);
            while (child.hasChild()) {
                child = child.getChildren().get(0);
            }
            return child;
        }
        else {
            return this;
        }

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
        if (tokens != null) {
            for (int i = 0; i < tokens.size(); i++) {
                if (i < tokens.size() - 1)
                    str += tokens.get(i).getValue() + " ";
                else
                    str += tokens.get(i).getValue();
            }
        }
        return str;
    }

    public String getASTString() {
        StringBuilder ASTString = new StringBuilder();
        VisitAST(this, ASTString, 0);
        return ASTString.toString();
    }

    private void VisitAST(ASTNode node, StringBuilder ASTString, int depth) {
        addASTStructure(depth, ASTString);
        ASTString.append(node.getClass() + ":" + node.toString() + "\n");
        for (ASTNode child : node.getChildren()) {
            if (child.getClass() == node.getClass())
                VisitAST(child, ASTString, depth);
            else
                VisitAST(child, ASTString, depth + 1);
        }
    }

    private void addASTStructure(int depth, StringBuilder ASTString) {
        for (int i = 1; i <= depth; i++) {
            ASTString.append("--");
        }
    }
}
