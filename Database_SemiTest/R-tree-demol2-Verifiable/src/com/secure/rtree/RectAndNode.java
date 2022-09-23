package com.secure.rtree;

import java.util.Stack;

public class RectAndNode {
    private Stack<Rectangle> rect;
    private Stack<RTNode> node;

    public RectAndNode() {
    }

    public RectAndNode(Stack<Rectangle> rect, Stack<RTNode> node) {
        this.rect = rect;
        this.node = node;
    }

    public Stack<Rectangle> getRect() {
        return rect;
    }

    public void setRect(Stack<Rectangle> rect) {
        this.rect = rect;
    }

    public Stack<RTNode> getNode() {
        return node;
    }

    public void setNode(Stack<RTNode> node) {
        this.node = node;
    }
}
