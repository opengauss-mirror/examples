package Generator;

import Interface.ColumnType;
import Lexer.Token;
import Parser.AST.ASTNode;
import Parser.AST.AlterTable.AlterAddColumnNode;
import Parser.AST.AlterTable.AlterModifyColumnNode;
import Parser.AST.AlterTable.AlterNode;
import Parser.AST.CreateTable.ColumnNode;
import Parser.AST.CreateTable.CreateTabNode;
import Exception.GenerateFailedException;
import Parser.AST.Delete.DeleteNode;
import Parser.AST.Drop.DropNode;
import Parser.AST.Drop.DropOptionNode;
import Parser.AST.Insert.InsertNode;
import Parser.AST.Join.JoinConditionNode;
import Parser.AST.Join.JoinSourceTabNode;
import Parser.AST.Select.SelectNode;
import Parser.AST.Select.SelectObjNode;
import Parser.AST.Update.UpdateNode;
import Parser.AST.Update.UpdateObjNode;
import Parser.AST.View.ViewCreateNode;
import Parser.OracleParser;

import java.util.ArrayList;
import java.util.List;

public class OpenGaussGenerator {
    private ASTNode node;
    public OpenGaussGenerator(ASTNode node) {
        this.node = node;
    }

    public void setNode(ASTNode node) {
        this.node = node;
    }

    public String generate() {
        if (node instanceof CreateTabNode) {
            return GenCreatTableSQL(node);
        }
        else if (node instanceof InsertNode) {
            return GenInsertSQL(node);
        }
        else if (node instanceof DropNode) {
            return GenDropTableSQL(node);
        }
        else if (node instanceof SelectNode) {
            return GenSelectSQL(node);
        }
        else if (node instanceof JoinSourceTabNode) {
            return GenJoinSQL(node);
        }
        else if (node instanceof UpdateNode) {
            return GenUpdateSQL(node);
        }
        else if (node instanceof DeleteNode) {
            return GenDeleteSQL(node);
        }
        else if (node instanceof AlterNode) {
            return GenAlterSQL(node);
        }
        else if (node instanceof ViewCreateNode) {
            return GenCreateViewSQL(node);
        }
        else {
            try {
                throw new GenerateFailedException("Root node:" + node.getClass() + "(Unsupported node type!)");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private String GenCreatTableSQL(ASTNode node) {
        // type convert
        visitCrt(node);
//        System.out.println(node.getASTString());
        return node.toQueryString();
    }

    private String GenInsertSQL(ASTNode node) {
        // Insert statements do not need to be converted for the time being
        return node.toQueryString();
    }

    private String GenDropTableSQL(ASTNode node) {
        visitDrop(node);
        return node.toQueryString();
    }

    private String GenSelectSQL(ASTNode node) {
        visitSelect(node);
//        System.out.println(node.getASTString());
        return node.toQueryString();
    }

    private String GenJoinSQL(ASTNode node) {
        visitJoin(node);
        return node.toQueryString();
    }

    private String GenUpdateSQL(ASTNode node) {
        visitUpdate(node);
//        System.out.println(node.getASTString());
        return node.toQueryString();
    }

    private String GenDeleteSQL(ASTNode node) {
        return node.toQueryString();
    }

    private String GenAlterSQL(ASTNode node) {
        visitAlter(node);
//        System.out.println(node.getASTString());
        return node.toQueryString();
    }

    private String GenCreateViewSQL(ASTNode node) {
        visitCreateView(node);
        return node.toQueryString();
    }

    private void visitCrt(ASTNode node) {
        if (node instanceof ColumnNode) {
            ColumnTypeConvert((ColumnNode) node);
        }
        for (ASTNode child : node.getChildren()) {
            visitCrt(child);
        }
    }

    private void visitDrop(ASTNode node) {
        try {
            if (node instanceof DropOptionNode) {
                List<Token> tokens = new ArrayList<>();
                tokens.add(new Token(Token.TokenType.KEYWORD, "CASCADE"));
                tokens.add(new Token(Token.TokenType.KEYWORD, "CONSTRAINTS"));
                if (node.tokensEqual(tokens)) {
                    throw new GenerateFailedException("Unsupported type:" + node.toString() + "(OpenGauss doesn't support the keyword -- " + node.toString() + " or have any expression that keeps the same semantic!)");
                }
            }
        }
        catch (GenerateFailedException e) {
            e.printStackTrace();
        }
        for (ASTNode child : node.getChildren()) {
            visitDrop(child);
        }
    }

    private void visitJoin(ASTNode node) {
        if (node instanceof JoinConditionNode) {
           if (((JoinConditionNode) node).getKeyword().equalsIgnoreCase("USING")) {
               ((JoinConditionNode) node).setKeyword("ON");
           }
        }
        for (ASTNode child : node.getChildren()) {
            visitJoin(child);
        }
    }

    private void visitSelect(ASTNode node) {
        if (node instanceof JoinSourceTabNode) {
            visitJoin(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitSelect(child);
        }
    }

    private void visitUpdate(ASTNode node) {
        if (node instanceof JoinSourceTabNode) {
            visitJoin(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitUpdate(child);
        }
    }

    private void visitAlter(ASTNode node) {
        if (node instanceof AlterAddColumnNode) {
            ColumnTypeConvert((AlterAddColumnNode) node);
        }
        else if (node instanceof AlterModifyColumnNode) {
            ColumnTypeConvert((AlterModifyColumnNode) node);
        }
        for (ASTNode child : node.getChildren()) {
            visitAlter(child);
        }
    }

    private void visitCreateView(ASTNode node) {
        if (node instanceof SelectNode) {
            visitSelect(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitCreateView(child);
        }
    }

    private void ColumnTypeConvert(ColumnType node) {
        // type convert
        if (node.getType().getValue().equalsIgnoreCase("NUMBER")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "NUMERIC"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)NUMBER\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().replace("NUMBER", "DECIMAL")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("VARCHAR2")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "VARCHAR"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)VARCHAR2\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().replace("VARCHAR2", "VARCHAR")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("RAW")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "BYTEA"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)RAW\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().replace("RAW", "BYTEA")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("BINARY_INTEGER")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "INTEGER"));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("NCHAR")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "VARCHAR"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)NCHAR\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().replace("NCHAR", "VARCHAR")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("NVARCHAR2")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "VARCHAR"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)NVARCHAR2\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().replace("NVARCHAR2", "VARCHAR")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("NCLOB")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "TEXT"));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("INTERVAL YEAR TO MONTH")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "INTERVAL"));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("INTERVAL DAY TO SECOND")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "INTERVAL"));
            node.ResetTokensbyNameTypeConstraint();
        }

        // Impossible to convert
        if (
                node.getType().getValue().equalsIgnoreCase("TIMESTAMP WITH LOCAL TIME ZONE") ||
                        node.getType().getValue().equalsIgnoreCase("LONG RAW") ||
                        node.getType().getValue().equalsIgnoreCase("ROWID") ||
                        node.getType().getValue().equalsIgnoreCase("UROWID") ||
                        node.getType().getValue().equalsIgnoreCase("REF CURSOR")
        ) {
            try {
                throw new GenerateFailedException("Unsupported type:" + node.getType().getValue() + "(OpenGauss doesn't support the keyword -- " + node.getType().getValue() + " or have any expression that keeps the same semantic!)");
            }
            catch (GenerateFailedException e) {
                e.printStackTrace();
            }
        }
    }
}
