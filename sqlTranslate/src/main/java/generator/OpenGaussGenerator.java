package generator;

import config.CommonConfig;
import interfaces.DataType;
import lexer.OracleLexer;
import lexer.Token;
import parser.ast.ASTNode;
import parser.ast.alterTable.AlterAddColumnNode;
import parser.ast.alterTable.AlterModifyColumnNode;
import parser.ast.alterTable.AlterNode;
import parser.ast.createTable.ColumnNode;
import parser.ast.createTable.CreateTabNode;
import exception.GenerateFailedException;
import parser.ast.delete.DeleteNode;
import parser.ast.drop.DropNode;
import parser.ast.drop.DropOptionNode;
import parser.ast.exception.ExceptionActionNode;
import parser.ast.exception.ExceptionNode;
import parser.ast.function.FunctionColumnNode;
import parser.ast.function.FunctionEndNode;
import parser.ast.function.FunctionNode;
import parser.ast.function.FunctionRetDefNode;
import parser.ast.iFELSIF.IFConditionNode;
import parser.ast.insert.InsertNode;
import parser.ast.join.JoinConditionNode;
import parser.ast.join.JoinSourceTabNode;
import parser.ast.loop.ForNode;
import parser.ast.loop.LoopBodyNode;
import parser.ast.loop.LoopNode;
import parser.ast.loop.WhileNode;
import parser.ast.pl.PLBodyNode;
import parser.ast.pl.PLDeclareNode;
import parser.ast.pl.PLEndNode;
import parser.ast.pl.PLNode;
import parser.ast.procedure.*;
import parser.ast.select.SelectNode;
import parser.ast.trigger.*;
import parser.ast.update.UpdateNode;
import parser.ast.view.ViewCreateNode;

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
        else if (node instanceof IFConditionNode) {
            return GenIfElseSQL(node);
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
//        System.out.println(node.getASTString());
        return node.toQueryString();
    }

    private String GenIfElseSQL(ASTNode node) {
        visitIfElse(node);
        return node.toQueryString();
    }

    private void visitCrt(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof ColumnNode) {
                DataTypeConvert((ColumnNode) node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitCrt(child);
        }
    }

    private void visitDrop(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof DropOptionNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitDrop(child);
        }
    }

    private void visitJoin(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof JoinConditionNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitJoin(child);
        }
    }

    private void visitLoop(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof LoopBodyNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitLoop(child);
        }
    }

    private void visitIfElse(ASTNode node) {

        for (ASTNode child : node.getChildren()) {
            visitIfElse(child);
        }
    }

    private void visitException(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof ExceptionActionNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitException(child);
        }
    }

    private void visitSelect(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof JoinSourceTabNode) {
                visitJoin(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitSelect(child);
        }
    }

    private void visitUpdate(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof JoinSourceTabNode) {
                visitJoin(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitUpdate(child);
        }
    }

    private void visitAlter(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof AlterAddColumnNode) {
                DataTypeConvert((AlterAddColumnNode) node);
            }
            else if (node instanceof AlterModifyColumnNode) {
                DataTypeConvert((AlterModifyColumnNode) node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitAlter(child);
        }
    }

    private void visitCreateView(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof SelectNode) {
                visitSelect(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitCreateView(child);
        }
    }

    private void visitPL(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof ProcedureRetDefNode) {
                DataTypeConvert((ProcedureRetDefNode) node);
                CommonConvert(node);
            }
            else if (node instanceof ProcedureColumnNode) {
                DataTypeConvert((ProcedureColumnNode) node);
            }
            else if (node instanceof ProcedureEndNode) {
                CommonConvert(node);
            }
            else if (node instanceof ExceptionNode) {
                visitException(node);
            }
            else if (node instanceof ProcedurePLStatementNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitPL(child);
        }
    }

    private void visitFunc(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof FunctionRetDefNode) {
                CommonConvert(node);
            }
            else if (node instanceof ExceptionNode) {
                visitException(node);
            }
            else if (node instanceof FunctionColumnNode) {
                DataTypeConvert((FunctionColumnNode) node);
            }
            else if (node instanceof FunctionEndNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitFunc(child);
        }
    }

    private void visitTrigger(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof TriggerBodyNode) {
                CommonConvert(node);
            }
            else if (node instanceof TriggerConditionNode) {
                CommonConvert(node);
            }
            else if (node instanceof TriggerOptionNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitTrigger(child);
        }
    }

    private void visitPLSQL(ASTNode node) {
        if (CommonConfig.getSourceDB().equalsIgnoreCase("ORACLE-19C")
         && CommonConfig.getTargetDB().equalsIgnoreCase("OPENGAUSS-3.0.0")) {
            if (node instanceof PLNode) {
                CommonConvert(node);
            }
            else if (node instanceof PLDeclareNode) {
                DataTypeConvert((PLDeclareNode) node);
            }
            else if (node instanceof PLEndNode) {
                CommonConvert(node);
            }
            else if (node instanceof PLBodyNode) {
                CommonConvert(node);
            }
        }
        for (ASTNode child : node.getChildren()) {
            visitPLSQL(child);
        }
    }

    private void CommonConvert(ASTNode node) {
        if (node.checkExistsByRegex("(?i)DBMS_OUTPUT.PUT_LINE\\(.*?\\)")) {
            String printObj = "";
            int index = -1;
            for (Token token: node.getTokens()) {
                if (token.getValue().matches("(?i)DBMS_OUTPUT.PUT_LINE\\(.*?\\)")) {
                    index = node.getTokenIndexByRegex("(?i)DBMS_OUTPUT.PUT_LINE\\(.*?\\)");
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
                node.moveTokenByIndex(index);
                node.addTokensByIndex(index, tokens);
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
                    node.moveTokenByIndex(index);
                    node.addTokensByIndex(index, tokens);
                }
                else {
                    List<Token> tokens = new ArrayList<>();
                    tokens.add(new Token(Token.TokenType.KEYWORD, "RAISE"));
                    tokens.add(new Token(Token.TokenType.KEYWORD, "NOTICE"));
                    tokens.add(new Token(Token.TokenType.STRING, "'%'"));
                    tokens.add(new Token(Token.TokenType.SYMBOL, ","));
                    tokens.add(new Token(Token.TokenType.STRING, "'" + output + "'"));
                    node.moveTokenByIndex(index);
                    node.addTokensByIndex(index, tokens);
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
        if (node instanceof DropOptionNode) {
            try {
                List<Token> tokens = new ArrayList<>();
                tokens.add(new Token(Token.TokenType.KEYWORD, "CASCADE"));
                tokens.add(new Token(Token.TokenType.KEYWORD, "CONSTRAINTS"));
                if (node.tokensEqual(tokens)) {
                    throw new GenerateFailedException("Unsupported type:" + node.toString() + "(OpenGauss doesn't support the keyword -- " + node.toString() + " or have any expression that keeps the same semantic!)");
                }
            } catch (GenerateFailedException e) {
                e.printStackTrace();
            }
        }
        if (node instanceof JoinConditionNode) {
            if (((JoinConditionNode) node).getKeyword().equalsIgnoreCase("USING")) {
                ((JoinConditionNode) node).setKeyword("ON");
            }
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
