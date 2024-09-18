package Generator;

import Interface.DataType;
import Lexer.OracleLexer;
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
import Parser.AST.Exception.ExceptionActionNode;
import Parser.AST.Exception.ExceptionNode;
import Parser.AST.Function.FunctionColumnNode;
import Parser.AST.Function.FunctionEndNode;
import Parser.AST.Function.FunctionNode;
import Parser.AST.Function.FunctionRetDefNode;
import Parser.AST.Insert.InsertNode;
import Parser.AST.Join.JoinConditionNode;
import Parser.AST.Join.JoinSourceTabNode;
import Parser.AST.Loop.ForNode;
import Parser.AST.Loop.LoopBodyNode;
import Parser.AST.Loop.LoopNode;
import Parser.AST.Loop.WhileNode;
import Parser.AST.PL.PLBodyNode;
import Parser.AST.PL.PLDeclareNode;
import Parser.AST.PL.PLEndNode;
import Parser.AST.PL.PLNode;
import Parser.AST.Procedure.ProcedureColumnNode;
import Parser.AST.Procedure.ProcedureEndNode;
import Parser.AST.Procedure.ProcedureNode;
import Parser.AST.Procedure.ProcedureRetDefNode;
import Parser.AST.Select.SelectNode;
import Parser.AST.Trigger.*;
import Parser.AST.Update.UpdateNode;
import Parser.AST.View.ViewCreateNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        else if (node instanceof LoopNode || node instanceof WhileNode || node instanceof ForNode) {
            return GenLoopSQL(node);
        }
        else if (node instanceof ExceptionNode) {
            return GenExceptionSQL(node);
        }
        else if (node instanceof ProcedureNode) {
            return GenProcedureSQL(node);
        }
        else if (node instanceof FunctionNode) {
            return GenFunctionSQL(node);
        }
        else if (node instanceof TriggerNode) {
            return GenTriggerSQL(node);
        }
        else if (node instanceof PLNode) {
            return GenPLSQL(node);
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

    private String GenLoopSQL(ASTNode node) {
        visitLoop(node);
        return node.toQueryString();
    }

    private String GenExceptionSQL(ASTNode node) {
        visitException(node);
        return node.toQueryString();
    }

    private String GenProcedureSQL(ASTNode node) {
        visitPL(node);
//        System.out.println(node.getASTString());
        return node.toQueryString();
    }

    private String GenFunctionSQL(ASTNode node) {
        visitFunc(node);
        return node.toQueryString();
    }

    private String GenTriggerSQL(ASTNode node) {
        visitTrigger(node);
        return node.toQueryString();
    }

    private String GenPLSQL(ASTNode node) {
        visitPLSQL(node);
        return node.toQueryString();
    }

    private void visitCrt(ASTNode node) {
        if (node instanceof ColumnNode) {
            DataTypeConvert((ColumnNode) node);
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

    private void visitLoop(ASTNode node) {
        if (node instanceof LoopBodyNode) {
            PLConvert(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitLoop(child);
        }
    }

    private void visitException(ASTNode node) {
        if (node instanceof ExceptionActionNode) {
            PLConvert(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitException(child);
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
            DataTypeConvert((AlterAddColumnNode) node);
        }
        else if (node instanceof AlterModifyColumnNode) {
            DataTypeConvert((AlterModifyColumnNode) node);
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

    private void visitPL(ASTNode node) {
        if (node instanceof ProcedureRetDefNode) {
            DataTypeConvert((ProcedureRetDefNode) node);
            PLConvert(node);
        }
        else if (node instanceof ProcedureColumnNode) {
            DataTypeConvert((ProcedureColumnNode) node);
        }
        else if (node instanceof ProcedureEndNode) {
            PLConvert(node);
        }
        else if (node instanceof ExceptionNode) {
            visitException(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitPL(child);
        }
    }

    private void visitFunc(ASTNode node) {
        if (node instanceof FunctionRetDefNode) {
            PLConvert(node);
        }
        else if (node instanceof ExceptionNode) {
            visitException(node);
        }
        else if (node instanceof FunctionColumnNode) {
            DataTypeConvert((FunctionColumnNode) node);
        }
        else if (node instanceof FunctionEndNode) {
            PLConvert(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitFunc(child);
        }
    }

    private void visitTrigger(ASTNode node) {
        if (node instanceof TriggerBodyNode) {
            PLConvert(node);
        }
        else if (node instanceof TriggerConditionNode) {
            PLConvert(node);
        }
        else if (node instanceof TriggerOptionNode) {
            PLConvert(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitTrigger(child);
        }
    }

    private void visitPLSQL(ASTNode node) {
        if (node instanceof PLNode) {
            PLConvert(node);
        }
        else if (node instanceof PLDeclareNode) {
            DataTypeConvert((PLDeclareNode) node);
        }
        else if (node instanceof PLEndNode) {
            PLConvert(node);
        }
        else if (node instanceof PLBodyNode) {
            PLConvert(node);
        }
        for (ASTNode child : node.getChildren()) {
            visitPLSQL(child);
        }
    }

    private void PLConvert(ASTNode node) {
        if (node.checkExistsByRegex("(?i)DBMS_OUTPUT.PUT_LINE\\(.*?\\)")) {
            String printObj = "";
            for (Token token: node.getTokens()) {
                if (token.getValue().matches("(?i)DBMS_OUTPUT.PUT_LINE\\(.*?\\)")) {
                    Pattern pattern = Pattern.compile("\\(([^()]*)\\)");
                    Matcher matcher = pattern.matcher(token.getValue());
                    while (matcher.find()) {
                        printObj = matcher.group(1);
                    }
                    break;
                }
            }
            if (!printObj.contains("||")) {
                List<Token> tokens = new ArrayList<>();
                tokens.add(new Token(Token.TokenType.KEYWORD, "RAISE"));
                tokens.add(new Token(Token.TokenType.KEYWORD, "NOTICE"));
                tokens.add(new Token(Token.TokenType.STRING, "'%'"));
                tokens.add(new Token(Token.TokenType.SYMBOL, ","));
                tokens.add(new Token(Token.TokenType.IDENTIFIER, printObj));
                tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
                node.setTokens(tokens);
            }
            else {
                OracleLexer lexer = new OracleLexer(printObj.replace("||", " "));
                String output = "";
                List<Token> outputObj = new ArrayList<>();
                for (Token token: lexer.getTokens()) {
                    if (token.hasType(Token.TokenType.STRING)) {
                        output += token.getValue().replace("'", "");
                    }
                    else if (token.hasType(Token.TokenType.IDENTIFIER) || token.hasType(Token.TokenType.KEYWORD)) {
                        output += "%";
                        outputObj.add(token);
                    }
                    else {
                        continue;
                    }
                }
                if (!outputObj.isEmpty()) {
                    List<Token> tokens = new ArrayList<>();
                    tokens.add(new Token(Token.TokenType.KEYWORD, "RAISE"));
                    tokens.add(new Token(Token.TokenType.KEYWORD, "NOTICE"));
                    tokens.add(new Token(Token.TokenType.STRING, "'" + output + "'"));
                    for (Token token: outputObj) {
                        tokens.add(new Token(Token.TokenType.SYMBOL, ","));
                        tokens.add(token);
                    }
                    tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
                    node.setTokens(tokens);
                }
                else {
                    List<Token> tokens = new ArrayList<>();
                    tokens.add(new Token(Token.TokenType.KEYWORD, "RAISE"));
                    tokens.add(new Token(Token.TokenType.KEYWORD, "NOTICE"));
                    tokens.add(new Token(Token.TokenType.STRING, "'%'"));
                    tokens.add(new Token(Token.TokenType.SYMBOL, ","));
                    tokens.add(new Token(Token.TokenType.STRING, "'" + output + "'"));
                    tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
                    node.setTokens(tokens);
                }

            }
        }
        if (node instanceof ProcedureRetDefNode) {
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(Token.TokenType.KEYWORD, "AS"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "$$"));
            for (Token token: node.getTokens()) {
                if (token.hasType(Token.TokenType.KEYWORD) && token.getValue().equalsIgnoreCase("IS")) {
                    tokens.add(new Token(Token.TokenType.KEYWORD, "DECLARE"));
                }
                else {
                    tokens.add(token);
                }
            }
            node.setTokens(tokens);
        }
        if (node instanceof ProcedureEndNode) {
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(Token.TokenType.KEYWORD, "END"));
            tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "$$"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "LANGUAGE"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "plpgsql"));
            tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
            node.setTokens(tokens);
        }
        if (node instanceof FunctionEndNode) {
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(Token.TokenType.KEYWORD, "END"));
            tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "$$"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "LANGUAGE"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "plpgsql"));
            tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
            node.setTokens(tokens);
        }
        if (node instanceof FunctionRetDefNode) {
            List<Token> tokens = new ArrayList<>();
            for (Token token: node.getTokens()) {
                if (token.hasType(Token.TokenType.KEYWORD) && token.getValue().equalsIgnoreCase("RETURN")) {
                    tokens.add(new Token(Token.TokenType.KEYWORD, "RETURNS"));
                }
                else if (token.hasType(Token.TokenType.KEYWORD) && token.getValue().equalsIgnoreCase("IS")) {
                    tokens.add(new Token(Token.TokenType.KEYWORD, "AS"));
                    tokens.add(new Token(Token.TokenType.KEYWORD, "$$"));
                }
                else {
                    tokens.add(token);
                }
            }
            node.setTokens(tokens);
        }
        if (node instanceof TriggerOptionNode) {
            try {
                throw new GenerateFailedException("Unsupported type:" + node.toString() + "(OpenGauss doesn't support the keyword -- " + node.toString() + " or have any expression that keeps the same semantic!)");
            }
            catch (GenerateFailedException e) {
                e.printStackTrace();
            }
        }
        if (node instanceof TriggerConditionNode) {
            if (((TriggerConditionNode) node).getCondition().hasType(Token.TokenType.KEYWORD) &&
                    ((TriggerConditionNode) node).getCondition().getValue().equalsIgnoreCase("INSTEAD OF")) {
                try {
                    throw new GenerateFailedException("Unsupported type:" + node.toString() + "(OpenGauss doesn't support the keyword -- " + node.toString() + " or have any expression that keeps the same semantic!)");
                }
                catch (GenerateFailedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (node instanceof PLNode) {
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(Token.TokenType.KEYWORD, "DO"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "$$"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "DECLARE"));
            node.setTokens(tokens);
        }
        if (node instanceof PLEndNode) {
            List<Token> tokens = new ArrayList<>();
            tokens.add(new Token(Token.TokenType.KEYWORD, "END"));
            tokens.add(new Token(Token.TokenType.KEYWORD, "$$"));
            tokens.add(new Token(Token.TokenType.SYMBOL, ";"));
            node.setTokens(tokens);
        }
    }


    private void DataTypeConvert(DataType node) {
        // type convert
        if (node.getType().getValue().equalsIgnoreCase("NUMBER")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "NUMERIC"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)NUMBER\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().toUpperCase().replace("NUMBER", "DECIMAL")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("VARCHAR2")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "VARCHAR"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)VARCHAR2\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().toUpperCase().replace("VARCHAR2", "VARCHAR")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("RAW")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "BYTEA"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)RAW\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().toUpperCase().replace("RAW", "BYTEA")));
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
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().toUpperCase().replace("NCHAR", "VARCHAR")));
            node.ResetTokensbyNameTypeConstraint();
        }

        if (node.getType().getValue().equalsIgnoreCase("NVARCHAR2")) {
            node.setType(new Token(Token.TokenType.KEYWORD, "VARCHAR"));
            node.ResetTokensbyNameTypeConstraint();
        }
        if (node.getType().getValue().matches("(?i)NVARCHAR2\\(.*?\\)")) {
            node.setType(new Token(Token.TokenType.KEYWORD, node.getType().getValue().toUpperCase().replace("NVARCHAR2", "VARCHAR")));
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
