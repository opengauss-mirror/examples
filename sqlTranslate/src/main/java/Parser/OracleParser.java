package Parser;

import Lexer.OracleLexer;
import Lexer.Token;
import Parser.AST.ASTNode;
import Exception.ParseFailedException;
import Parser.AST.AlterTable.*;
import Parser.AST.CaseWhen.CaseConditionNode;
import Parser.AST.CaseWhen.CaseElseNode;
import Parser.AST.CaseWhen.CaseEndNode;
import Parser.AST.CaseWhen.CaseThenNode;
import Parser.AST.CreateTable.*;
import Parser.AST.Delete.DeleteConditionNode;
import Parser.AST.Delete.DeleteEndNode;
import Parser.AST.Delete.DeleteNode;
import Parser.AST.Delete.DeleteObjNode;
import Parser.AST.Drop.DropEndNode;
import Parser.AST.Drop.DropObjNameNode;
import Parser.AST.Drop.DropNode;
import Parser.AST.Drop.DropOptionNode;
import Parser.AST.Exception.*;
import Parser.AST.Function.*;
import Parser.AST.IFELSIF.*;
import Parser.AST.Insert.InsertDataNode;
import Parser.AST.Insert.InsertEndNode;
import Parser.AST.Insert.InsertNode;
import Parser.AST.Insert.InsertObjNode;
import Parser.AST.Join.*;
import Parser.AST.Loop.*;
import Parser.AST.Procedure.*;
import Parser.AST.Select.*;
import Parser.AST.Trigger.*;
import Parser.AST.Update.*;
import Parser.AST.View.ViewCreateNode;
import Parser.AST.View.ViewEndNode;
import Parser.AST.View.ViewNameNode;
import Parser.AST.View.ViewTargetNode;

import java.util.Stack;

import java.util.ArrayList;
import java.util.List;

public class OracleParser {
    private OracleLexer lexer;
    public OracleParser(OracleLexer lexer) {
        this.lexer = lexer;
    }
    public ASTNode parse()
    {
        try {
            // check if the input is a create table statement
            if ((lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("TABLE")) ||
                    (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(2).getValue().equalsIgnoreCase("TEMPORARY") && lexer.getTokens().get(3).getValue().equalsIgnoreCase("TABLE"))) {
                return parseCreateTab(lexer.getTokens());
            } else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("INSERT")) {
                return parseInsert(lexer.getTokens());
            } else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("DROP")) {
                return parseDrop(lexer.getTokens());
            } else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("SELECT")) {
                return parseSelect(lexer.getTokens());
            } else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("UPDATE")) {
                return parseUpdate(lexer.getTokens());
            } else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("DELETE")) {
                return parseDelete(lexer.getTokens());
            } else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("ALTER")) {
                return parseAlterTable(lexer.getTokens());
            } else if (
                    (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("OR") && lexer.getTokens().get(2).getValue().equalsIgnoreCase("REPLACE") && lexer.getTokens().get(3).getValue().equalsIgnoreCase("VIEW"))
                            || (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("VIEW"))
            ) {
                return parseCreateView(lexer.getTokens());
            } else if (
                    lexer.getTokens().get(0).getValue().equalsIgnoreCase("FOR")
                    || lexer.getTokens().get(0).getValue().equalsIgnoreCase("LOOP")
                    || lexer.getTokens().get(0).getValue().equalsIgnoreCase("WHILE")
            ) {
                return parseLoop(lexer.getTokens());
            } else if (
                    (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("PROCEDURE"))
                    || (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("OR")  && lexer.getTokens().get(2).getValue().equalsIgnoreCase("REPLACE") && lexer.getTokens().get(3).getValue().equalsIgnoreCase("PROCEDURE"))
            )
            {
                return parseProcedure(lexer.getTokens());
            } else if (
                    (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("FUNCTION"))
                            || (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("OR")  && lexer.getTokens().get(2).getValue().equalsIgnoreCase("REPLACE") && lexer.getTokens().get(3).getValue().equalsIgnoreCase("FUNCTION"))
            )
            {
                return parseFunction(lexer.getTokens());
            } else if (
                    (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("TRIGGER"))
                            || (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("OR")  && lexer.getTokens().get(2).getValue().equalsIgnoreCase("REPLACE") && lexer.getTokens().get(3).getValue().equalsIgnoreCase("TRIGGER"))
            )
            {
                return parseTrigger(lexer.getTokens());
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed!--Unsupport SQL:" + lexer.getTokens().get(0).getValue());
                } catch (ParseFailedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * CREATE TEMPORARY? TABLE _table ( (_column _type column_constraint), (_column _type column_constraint)* table_constraint?)
     */
    private ASTNode parseCreateTab(List<Token> parseTokens) {
        List <Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));
        ASTNode root = new CreateTabNode(tokens);
        ASTNode currentNode = null;
        for (int i = 1; i < parseTokens.size(); i++) {
//            System.out.println("Parsing the token: " + parseTokens.get(i).getValue());
            if (i == 1 && parseTokens.get(i).getValue().equalsIgnoreCase("TABLE")) {
                tokens = new ArrayList<>();

                tokens.add(parseTokens.get(i));
                try {
                    tokens.add(parseTokens.get(i + 1));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
                i++;
                i++; // pass the "("
                ASTNode child = new TableNode(tokens);
                root.addChild(child);
                currentNode = child;
            }
            else if (i == 1 && !parseTokens.get(i).getValue().equalsIgnoreCase("TABLE")){
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                for (int j = i + 1; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).getValue().equalsIgnoreCase("TABLE")) {
                        ASTNode child = new TableTypeNode(tokens);
                        root.addChild(child);
                        currentNode = child;
                        tokens = new ArrayList<>();
                        tokens.add(parseTokens.get(j));
                        try {
                            tokens.add(parseTokens.get(j + 1));
                        }
                        catch (ParseFailedException e) {
                            e.printStackTrace();
                        }
                        i = j + 2; // pass the "("
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }

                ASTNode child = new TableNode(tokens);
                currentNode.addChild(child);
                currentNode = child;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                // Token.TokenType.IDENTIFIER ... , -> column node
                tokens = new ArrayList<>();
                ColumnNode child = new ColumnNode();
                child.setName(parseTokens.get(i));
                tokens.add(parseTokens.get(i));
                List <Token> constraint = new ArrayList<>();
                for (int j = i + 1; j < parseTokens.size(); j++) {
                    if (j == i + 1) {
                        child.setType(parseTokens.get(j));
                    }
                    // Check () or REFERENCES other_table(other_column)
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) &&
                            (parseTokens.get(j).getValue().equalsIgnoreCase("REFERENCES")
                                    || parseTokens.get(j).getValue().equalsIgnoreCase("CHECK"))) {
                        tokens.add(parseTokens.get(j));
                        constraint.add(parseTokens.get(j));
                        Stack<String> stack = new Stack<>();
                        for (int k = j + 1; k < parseTokens.size(); k++) {
                            tokens.add(parseTokens.get(k));
                            constraint.add(parseTokens.get(k));
                            if (parseTokens.get(k).getValue().equals("(")) {
                                stack.push("(");
                                for (int t = k + 1; t < parseTokens.size(); t++) {
                                    tokens.add(parseTokens.get(t));
                                    constraint.add(parseTokens.get(t));
                                    if (parseTokens.get(t).getValue().equals("(")) {
                                        stack.push("(");
                                    }
                                    else if (parseTokens.get(t).getValue().equals(")")) {
                                        stack.pop();
                                        if (stack.empty()) {
                                            i = t;
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                    if ((parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(",")) ||
                            (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(")")) ) {
                        i = j;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                    if (j != i + 1) {
                        constraint.add(parseTokens.get(j));
                    }
                }
                child.setTokens(tokens);
                child.setConstraint(constraint);
                currentNode.addChild(child);
                currentNode = child;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD)
                    && parseTokens.get(i).getValue().equalsIgnoreCase("CONSTRAINT")) {
                // Table constraint
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                for (int j = i + 1; j < parseTokens.size(); j++) {
                    /**
                     * CONSTRAINT pk_example PRIMARY KEY (column1, column2)
                     * CONSTRAINT uk_example UNIQUE (column1)
                     * CONSTRAINT fk_example FOREIGN KEY (column1) REFERENCES other_table(column2)
                     * CONSTRAINT chk_example CHECK (column1 > 0)
                     */
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) &&
                            (parseTokens.get(j).getValue().equalsIgnoreCase("UNIQUE")
                                    || parseTokens.get(j).getValue().equalsIgnoreCase("CHECK")
                                    || parseTokens.get(j).getValue().equalsIgnoreCase("PRIMARY KEY")
                                    || parseTokens.get(j).getValue().equalsIgnoreCase("FOREIGN KEY")
                                    || parseTokens.get(j).getValue().equalsIgnoreCase("REFERENCES"))) {
                        tokens.add(parseTokens.get(j));
                        Stack<String> stack = new Stack<>();
                        for (int k = j + 1; k < parseTokens.size(); k++) {
                            tokens.add(parseTokens.get(k));
                            if (parseTokens.get(k).getValue().equals("(")) {
                                stack.push("(");
                                for (int t = k + 1; t < parseTokens.size(); t++) {
                                    tokens.add(parseTokens.get(t));
                                    if (parseTokens.get(t).getValue().equals("(")) {
                                        stack.push("(");
                                    }
                                    else if (parseTokens.get(t).getValue().equals(")")) {
                                        stack.pop();
                                        if (stack.empty()) {
                                            i = t;
                                            j = t;
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        continue;
                    }
                    if ((parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(",")) ||
                            (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(")")) ) {
                        i = j;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new TableConstraintNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode EndNode = new CRTEndNode(tokens);
                currentNode.addChild(EndNode);
                currentNode = EndNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(",")) {
                continue;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
//                System.out.println("Fail to parse:" + parseTokens.get(i).getValue());
                try {
                    throw new ParseFailedException("Parse failed!--" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }
        return root;
    }

    /**
     * INSERT INTO table_name VALUES (value1, value2, ...)
     */
    private ASTNode parseInsert(List<Token> parseTokens) {
        List <Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));
        try {
            tokens.add(parseTokens.get(1));
        }
        catch (ParseFailedException e) {
            e.printStackTrace();
        }
        ASTNode root = new InsertNode(tokens);
        ASTNode currentNode = root;

        for (int i = 2; i < parseTokens.size(); i++) {
            if (parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) { // table name
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                if (i + 1 < parseTokens.size() && ! (parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("VALUES"))) {
                    for (int j = i + 1; j < parseTokens.size(); j++) {
                        tokens.add(parseTokens.get(j));
                        if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(")")) {
                            i = j;
                            break;
                        }
                    }
                }
                ASTNode childNode = new InsertObjNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("VALUES")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                for (int j = i + 1; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new InsertDataNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new InsertEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed!");
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }

        }

        return root;
    }

    /**
     * DROP TABLE _table (CASCADE CONSTRAINTS)? | DROP VIEW _view
     */
    private ASTNode parseDrop(List<Token> parseTokens) {
        List <Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));
        try {
            tokens.add(parseTokens.get(1));
        }
        catch (ParseFailedException e) {
            e.printStackTrace();
        }
        ASTNode root = new DropNode(tokens);
        ASTNode currentNode = root;

        for (int i = 2; i < parseTokens.size(); i++) {
            if (parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new DropObjNameNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("CASCADE")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                if (i + 1 < parseTokens.size() && (parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equals("CONSTRAINTS"))) {
                    tokens.add(parseTokens.get(i + 1));
                    i++;
                }
                ASTNode childNode = new DropOptionNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new DropEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed!");
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

    /**
     * SELECT DISTINCT? select_obj FROM select_tab where_clause? select_option? union_clause?
     */
    private ASTNode parseSelect(List<Token> parseTokens) {
        List <Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));
        ASTNode root = new SelectNode(tokens);
        ASTNode currentNode = root;
        for(int i = 1; i < parseTokens.size(); i++) {
            // match select_obj
            if (i == 1 && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("DISTINCT")) {
                SelectContentNode childNode = new SelectContentNode();
                childNode.setIsDistinct(parseTokens.get(i).getValue());
                tokens = new ArrayList<>();
                for (int j = i + 1; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("FROM")) {
                        i = j;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                childNode.setTokens(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (i == 1 && (!parseTokens.get(i).hasType(Token.TokenType.KEYWORD) || !parseTokens.get(i).getValue().equalsIgnoreCase("DISTINCT"))) {
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("FROM")) {
                        i = j;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new SelectContentNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match select_tab (possible last token: ; | GROUP BY | ORDER BY | HAVING | WHERE | UNION)
            else if (currentNode instanceof SelectContentNode) {
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (
                            (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("WHERE"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("GROUP BY"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("ORDER BY"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("HAVING"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("UNION"))
                            || (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";"))
                    ) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                if (tokens.contains(new Token(Token.TokenType.KEYWORD, "JOIN"))) {
                    ASTNode joinRootNode = parseJoin(tokens);
                    currentNode.addChild(joinRootNode);
                    currentNode = joinRootNode.getDeepestChild();
                }
                else {
                    ASTNode childNode = new SelectObjNode(tokens);
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
            }
            // match where_clause (possible last token: ; | GROUP | ORDER | HAVING | UNION)
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("WHERE")) {
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (
                            (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("GROUP BY"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("ORDER BY"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("HAVING"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("UNION"))
                            || (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";"))
                    ) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new SelectWhereClauseNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match select_option
            else if (
                    (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("GROUP BY"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ORDER BY"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("HAVING"))
            ) {
                tokens = new ArrayList<>();
                String optionName = parseTokens.get(i).getValue();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (
                            j != i && !parseTokens.get(j).getValue().equalsIgnoreCase(optionName) &&
                            ((parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("UNION"))
                          || (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";"))
                          || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("GROUP BY"))
                          || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("ORDER BY"))
                          || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("HAVING")))
                    ) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new SelectOptionNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match union_clause
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("UNION")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new SelectUnionNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
                List <Token> unionTokens = parseTokens.subList(i + 1, parseTokens.size());
                ASTNode unionChildNode = parseSelect(unionTokens);
                currentNode.addChild(unionChildNode);
                currentNode = unionChildNode;
                break;
            }
            // match ;
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new SelectEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed!--" + parseTokens.get(i).getValue());
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

    /**
     * IF condition1 THEN ...
     * (ELSIF condition2 THEN ...)*
     * [ELSE...]
     * END IF;
     * @param parseTokens should start with IF
     */
    public static ASTNode parseIFELSE(List<Token> parseTokens) {
        ASTNode root  = new IFConditionNode();
        ASTNode currentNode = root;
        for (int i = 0; i < parseTokens.size(); i++) {
            // match IF condition
            if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("IF")) {
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("THEN")) {
                        i = j - 1;
                        break;
                    }
                    root.addToken(parseTokens.get(j));
                }
            }
            // match IF action
            else if (currentNode instanceof IFConditionNode && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("THEN")) {
                ASTNode childNode = new IFActionNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match ELSIF condition
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ELSIF")) {
                ASTNode childNode = new ELSIFConditionNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("THEN")) {
                        i = j - 1;
                        break;
                    }
                    childNode.addToken(parseTokens.get(j));
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match ELSIF action
            else if (currentNode instanceof ELSIFConditionNode && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("THEN")) {
                ASTNode childNode = new ELSIFActionNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match ELSE
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ELSE")) {
                ASTNode childNode = new ELSENode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            //match end if;
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) {
                ASTNode childNode = new EndIFNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed!--" + parseTokens.get(i).getValue());
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }


    /**
     * CASE WHEN expr THEN expr [ELSE expr] END
     * @param parseTokens should start with CASE
     */
    public static ASTNode parseCaseWhen(List<Token> parseTokens) {
        List <Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));

        try {
            tokens.add(parseTokens.get(1));
        }
        catch (ParseFailedException e) {
            e.printStackTrace();
        }
        int currentIndex = 2;
        for (int i = currentIndex; i < parseTokens.size(); i++) {
            if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("THEN")) {
                currentIndex = i;
                break;
            }
            tokens.add(parseTokens.get(i));
        }
        ASTNode root = new CaseConditionNode(tokens);
        ASTNode currentNode = root;

        tokens = new ArrayList<>();
        for (int i = currentIndex; i < parseTokens.size(); i++) {
            if ( (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ELSE"))
            || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) ) {
                currentIndex = i;
                break;
            }
            tokens.add(parseTokens.get(i));
        }
        ASTNode childNode = new CaseThenNode(tokens);
        currentNode.addChild(childNode);
        currentNode = childNode;

        tokens = new ArrayList<>();
        for (int i = currentIndex; i < parseTokens.size(); i++) {
            if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ELSE")) {
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("END")) {
                        childNode = new CaseElseNode(tokens);
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                childNode = new CaseEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
                break;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed!--" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

    /**
     * Loop_clauses
     * 1:LOOP
     *     --
     *     EXIT WHEN condition;
     * END LOOP;
     *
     * 2:WHILE condition LOOP
     *     --
     * END LOOP;
     *
     * 3:FOR counter IN start..stop LOOP
     *     --
     * END LOOP;
     *
     * @param parseTokens should start with LOOP | WHILE | FOR
     */
    public static ASTNode parseLoop(List<Token> parseTokens) {
        ASTNode root = null;
        ASTNode currentNode = null;
        for (int i = 0; i < parseTokens.size(); i++) {
            //match LOOP
            if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("LOOP")) {
                root = new LoopNode();
                root.addToken(parseTokens.get(i));
                currentNode = root;
            }
            // match WHILE condition LOOP
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("WHILE")) {
                root = new WhileNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    root.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equals("LOOP")) {
                        i = j;
                        break;
                    }
                }
                currentNode = root;

            }
            // match FOR counter IN start..stop LOOP
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("FOR")) {
                root = new ForNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    root.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equals("LOOP")) {
                        i = j;
                        break;
                    }
                }
                currentNode = root;
            }
            // match END LOOP;
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) {
                ASTNode childNode = new LoopEndNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match EXIT condition;
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("EXIT")) {
                ASTNode childNode = new LoopExitNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            //match LOOP BODY
            else if (
                    (currentNode == root && root instanceof LoopNode)
                    || (currentNode == root && root instanceof WhileNode)
                    || (currentNode == root && root instanceof ForNode)
            ) {
                ASTNode childNode = new LoopBodyNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (
                            parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equals("EXIT")
                                    || parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equals("END")
                    ) {
                        i = j - 1;
                        break;
                    }
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                        childNode = new LoopBodyNode();
                    }
                }
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed!--" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

    /**
     * Exception
     * For example: EXCEPTION
     *     -- custom exception
     *     WHEN e_custom_exception THEN
     *         DBMS_OUTPUT.PUT_LINE('Caught an exception: Custom exception raised');
     *     -- divide by zero
     *     WHEN ZERO_DIVIDE THEN
     *         DBMS_OUTPUT.PUT_LINE('Caught an exception: Division by zero');
     *     -- invalid number
     *     WHEN INVALID_NUMBER THEN
     *         DBMS_OUTPUT.PUT_LINE('Caught an exception: Invalid number');
     *     -- other exceptions
     *     WHEN OTHERS THEN
     *         DBMS_OUTPUT.PUT_LINE('Caught an exception: ' || SQLERRM);
     * @param parseTokens should start with Exception keyword
     * @return ASTNode will not contain 'END'
     */
    public static ASTNode parseException(List<Token> parseTokens) {
        ASTNode root = new ExceptionNode();
        root.addToken(parseTokens.get(0));
        ASTNode currentNode = root;
        for (int i = 1; i < parseTokens.size(); i++) {
            // match exception type
            if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("WHEN")) {
                if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.IDENTIFIER)) {
                    ASTNode childNode = new CustomExceptionNode();
                    for (int j = i; j < parseTokens.size(); j++) {
                        childNode.addToken(parseTokens.get(j));
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("THEN")) {
                            i = j;
                            break;
                        }
                    }
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("ZERO_DIVIDE")) {
                    ASTNode childNode = new Zero_divideNode();
                    for (int j = i; j < parseTokens.size(); j++) {
                        childNode.addToken(parseTokens.get(j));
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("THEN")) {
                            i = j;
                            break;
                        }
                    }
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("INVALID_NUMBER")) {
                    ASTNode childNode = new Invalid_numberNode();
                    for (int j = i; j < parseTokens.size(); j++) {
                        childNode.addToken(parseTokens.get(j));
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("THEN")) {
                            i = j;
                            break;
                        }
                    }
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("OTHERS")) {
                    ASTNode childNode = new OtherExceptionNode();
                    for (int j = i; j < parseTokens.size(); j++) {
                        childNode.addToken(parseTokens.get(j));
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("THEN")) {
                            i = j;
                            break;
                        }
                    }
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else {
                    try {
                        throw new ParseFailedException("Parse failed:There exists syntex error in the input sql!");
                    }
                    catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) {
                ASTNode childNode = new ExceptionEndNode();
                currentNode.addChild(childNode);
                currentNode = childNode;
                break;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                if (!(currentNode instanceof ExceptionEndNode)) {
                    ASTNode childNode = new ExceptionEndNode();
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else {
                    break;
                }
            }
            // match exception action
            else {
                ASTNode childNode = new ExceptionActionNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
        }

        return root;
    }

    /**
     * JOIN clause
     * For example: table1 t1 JOIN table2 t2 ON|USING t1.id = t2.id
     * @param parseTokens should start with _table
     */
    public static ASTNode parseJoin(List<Token> parseTokens) {
        List <Token> tokens = new ArrayList<>();
        int index = 0;
        boolean parseState = false;
        ASTNode currentNode = null;
        ASTNode root = null;
        // match table 1
        for (int i = index; i < parseTokens.size(); i++) {
            if (
                    (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("INNER JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("LEFT JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("RIGHT JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("FULL JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("LEFT OUTER JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("RIGHT OUTER JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("FULL OUTER JOIN"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("CROSS JOIN"))
            ) {
                index = i;
                parseState = true;
                break;
            }
            tokens.add(parseTokens.get(i));
        }
        if (parseState) {
            root = new JoinSourceTabNode(tokens);
            currentNode = root;
        }
        else {
            try {
                throw new ParseFailedException("Fail to parse:" + parseTokens.get(index));
            }
            catch (ParseFailedException e) {
                e.printStackTrace();
            }
        }

        // match join type
        if (parseState) {
            tokens = new ArrayList<>();
            tokens.add(parseTokens.get(index));
            index++;
            ASTNode childNode = new JoinTypeNode(tokens);
            currentNode.addChild(childNode);
            currentNode = childNode;
        }
        else {
            try {
                throw new ParseFailedException("Fail to parse:" + parseTokens.get(index));
            }
            catch (ParseFailedException e) {
                e.printStackTrace();
            }
        }

        // match table 2
        tokens = new ArrayList<>();
        parseState = false;
        for (int i = index; i < parseTokens.size(); i++) {
            if (
                    (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ON"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("USING"))
                    || (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equalsIgnoreCase(";"))
                    || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD))
            ) {
                index = i;
                parseState = true;
                break;
            }
            tokens.add(parseTokens.get(i));
        }
        if (parseState) {
            ASTNode childNode = new JoinTargetTabNode(tokens);
            currentNode.addChild(childNode);
            currentNode = childNode;
        }
        else {
            ASTNode childNode = new JoinTargetTabNode(tokens);
            currentNode.addChild(childNode);
            currentNode = childNode;
        }

        // match join condition
        if (parseState) {
            // match end
            if (parseTokens.get(index).hasType(Token.TokenType.SYMBOL) && parseTokens.get(index).getValue().equalsIgnoreCase(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(index));
                ASTNode childNode = new JoinEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match end with keyword
            else if (parseTokens.get(index).hasType(Token.TokenType.KEYWORD) && !parseTokens.get(index).getValue().equalsIgnoreCase("ON") && !parseTokens.get(index).getValue().equalsIgnoreCase("USING")) {
                ASTNode childNode = new JoinEndNode();
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else {
                parseState = false;
                JoinConditionNode joinConditionNode = new JoinConditionNode();
                joinConditionNode.setKeyword(parseTokens.get(index).getValue());
                index++;
                tokens = new ArrayList<>();
                for (int i = index; i < parseTokens.size(); i++) {
                    if (
                            parseTokens.get(i).hasType(Token.TokenType.KEYWORD)
                            || (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equalsIgnoreCase(";"))
                    )
                    {
                        index = i;
                        parseState = true;
                        break;
                    }
                    tokens.add(parseTokens.get(i));
                }
                if (parseState) {
                    joinConditionNode.setTokens(tokens);
                    currentNode.addChild(joinConditionNode);
                    currentNode = joinConditionNode;
                    // match end
                    if (parseTokens.get(index).hasType(Token.TokenType.SYMBOL) && parseTokens.get(index).getValue().equalsIgnoreCase(";")) {
                        tokens = new ArrayList<>();
                        tokens.add(parseTokens.get(index));
                        ASTNode childNode = new JoinEndNode(tokens);
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                    }
                    // match end with keyword
                    else if (parseTokens.get(index).hasType(Token.TokenType.KEYWORD)) {
                        ASTNode childNode = new JoinEndNode();
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                    }
                }
                else {
                    joinConditionNode.setTokens(tokens);
                    currentNode.addChild(joinConditionNode);
                    currentNode = joinConditionNode;
                    ASTNode childNode = new JoinEndNode();
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
            }
        }
        else {
            ASTNode childNode = new JoinEndNode();
            currentNode.addChild(childNode);
            currentNode = childNode;
        }

        return root;
    }

    /**
     * Update
     * For example: UPDATE table_name SET column1 = value1, column2 = value2, ... WHERE condition;
     *              | UPDATE view_name SET column1 = value1, column2 = value2, ... WHERE condition;
     */
    private ASTNode parseUpdate(List<Token> parseTokens) {
        List<Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));
        ASTNode root = new UpdateNode(tokens);
        ASTNode currentNode = root;
        for (int i = 1; i < parseTokens.size(); i++) {
            if (i == 1 && parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                // match update object
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("SET")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                if (tokens.contains(new Token(Token.TokenType.KEYWORD, "JOIN"))) {
                    ASTNode joinRootNode = parseJoin(tokens);
                    currentNode.addChild(joinRootNode);
                    currentNode = joinRootNode.getDeepestChild();
                }
                else {
                    ASTNode childNode = new UpdateObjNode(tokens);
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }

            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("SET")) {
                // match SET
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("WHERE")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new UpdateSetNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("WHERE")) {
                // match WHERE
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equalsIgnoreCase(";")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new UpdateWhereNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equalsIgnoreCase(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new UpdateEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed:" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

    /**
     * Delete
     * For example:
     *      DELETE FROM employees e
     *      WHERE e.department_id IN (
     *          SELECT d.department_id
     *          FROM departments d
     *          WHERE d.department_name = 'Sales'
     *      );
     */
    private ASTNode parseDelete(List<Token> parseTokens) {
        List<Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));
        try{
            tokens.add(parseTokens.get(1));
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        ASTNode root = new DeleteNode(tokens);
        ASTNode currentNode = root;
        for (int i = 2; i < parseTokens.size(); i++) {
            // match delete object
            if (i == 2 && parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("WHERE")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new DeleteObjNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match WHERE condition
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("WHERE")) {
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equalsIgnoreCase(";")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new DeleteConditionNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new DeleteEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed:" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

    /**
     * Alter table
     * Grammar: ALTER TABLE table_name action;
     * action = ADD column_name data_type [constraint]
     * | DROP COLUMN column_name
     * | MODIFY column_name data_type
     * | RENAME COLUMN old_column_name TO new_column_name
     * | ADD CONSTRAINT constraint_name constraint_definition
     * | DROP CONSTRAINT constraint_name
     * | RENAME old_table_name TO new_table_name
     */
    private ASTNode parseAlterTable(List<Token> parseTokens) {
        List<Token> tokens = new ArrayList<>();
        tokens.add(parseTokens.get(0));
        try{
            tokens.add(parseTokens.get(1));
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        ASTNode root = new AlterNode(tokens);
        ASTNode currentNode = root;
        for (int i = 2; i < parseTokens.size(); i++) {
            // match table
            if (i == 2 && parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (
                            (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("ADD"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("DROP"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("MODIFY"))
                            || (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("RENAME"))
                    ) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new AlterObjNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ADD")) {
                // add column
                if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.IDENTIFIER)) {
                    tokens = new ArrayList<>();
                    tokens.add(parseTokens.get(i));
                    i++;
                    boolean state = false;
                    AlterAddColumnNode child = new AlterAddColumnNode();
                    child.setName(parseTokens.get(i));
                    tokens.add(parseTokens.get(i));
                    List <Token> constraint = new ArrayList<>();
                    for (int j = i + 1; j < parseTokens.size(); j++) {
                        state = false;
                        if (j == i + 1) {
                            child.setType(parseTokens.get(j));
                        }
                        // Check () or REFERENCES other_table(other_column)
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) &&
                                (parseTokens.get(j).getValue().equalsIgnoreCase("REFERENCES")
                                        || parseTokens.get(j).getValue().equalsIgnoreCase("CHECK"))) {
                            tokens.add(parseTokens.get(j));
                            constraint.add(parseTokens.get(j));
                            Stack<String> stack = new Stack<>();
                            for (int k = j + 1; k < parseTokens.size(); k++) {
                                tokens.add(parseTokens.get(k));
                                constraint.add(parseTokens.get(k));
                                if (parseTokens.get(k).getValue().equals("(")) {
                                    stack.push("(");
                                    for (int t = k + 1; t < parseTokens.size(); t++) {
                                        tokens.add(parseTokens.get(t));
                                        constraint.add(parseTokens.get(t));
                                        if (parseTokens.get(t).getValue().equals("(")) {
                                            stack.push("(");
                                        }
                                        else if (parseTokens.get(t).getValue().equals(")")) {
                                            stack.pop();
                                            if (stack.empty()) {
                                                j = t;
                                                state = true;
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        if ((parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) ) {
                            i = j - 1;
                            break;
                        }
                        if (!state)
                            tokens.add(parseTokens.get(j));
                        if (!state && j != i + 1) {
                            constraint.add(parseTokens.get(j));
                        }
                    }
                    child.setTokens(tokens);
                    child.setConstraint(constraint);
                    currentNode.addChild(child);
                    currentNode = child;
                }
                // add constraint
                else if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("CONSTRAINT")) {
                    tokens = new ArrayList<>();
                    tokens.add(parseTokens.get(i));
                    i++;
                    tokens.add(parseTokens.get(i));
                    for (int j = i + 1; j < parseTokens.size(); j++) {
                        /**
                         * CONSTRAINT pk_example PRIMARY KEY (column1, column2)
                         * CONSTRAINT uk_example UNIQUE (column1)
                         * CONSTRAINT fk_example FOREIGN KEY (column1) REFERENCES other_table(column2)
                         * CONSTRAINT chk_example CHECK (column1 > 0)
                         */
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) &&
                                (parseTokens.get(j).getValue().equalsIgnoreCase("UNIQUE")
                                        || parseTokens.get(j).getValue().equalsIgnoreCase("CHECK")
                                        || parseTokens.get(j).getValue().equalsIgnoreCase("PRIMARY KEY")
                                        || parseTokens.get(j).getValue().equalsIgnoreCase("FOREIGN KEY")
                                        || parseTokens.get(j).getValue().equalsIgnoreCase("REFERENCES"))) {
                            tokens.add(parseTokens.get(j));
                            Stack<String> stack = new Stack<>();
                            for (int k = j + 1; k < parseTokens.size(); k++) {
                                tokens.add(parseTokens.get(k));
                                if (parseTokens.get(k).getValue().equals("(")) {
                                    stack.push("(");
                                    for (int t = k + 1; t < parseTokens.size(); t++) {
                                        tokens.add(parseTokens.get(t));
                                        if (parseTokens.get(t).getValue().equals("(")) {
                                            stack.push("(");
                                        }
                                        else if (parseTokens.get(t).getValue().equals(")")) {
                                            stack.pop();
                                            if (stack.empty()) {
                                                i = t;
                                                j = t;
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            continue;
                        }
                        if ((parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";"))) {
                            i = j - 1;
                            break;
                        }
                        tokens.add(parseTokens.get(j));
                    }
                    ASTNode childNode = new AlterAddConstraintNode(tokens);
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                // error sql
                else {
                    try {
                        throw new ParseFailedException("Parse failed:There exists syntex error in the input sql!");
                    }
                    catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }

            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("DROP")) {
                if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("COLUMN")) {
                    tokens = new ArrayList<>();
                    tokens.add(parseTokens.get(i));
                    for (int j = i + 1; j < parseTokens.size(); j++) {
                        if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equalsIgnoreCase(";")) {
                            i = j - 1;
                            break;
                        }
                        tokens.add(parseTokens.get(j));
                    }
                    ASTNode childNode = new AlterDropColumnNode(tokens);
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("CONSTRAINT")) {
                    tokens = new ArrayList<>();
                    tokens.add(parseTokens.get(i));
                    for (int j = i + 1; j < parseTokens.size(); j++) {
                        if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equalsIgnoreCase(";")) {
                            i = j - 1;
                            break;
                        }
                        tokens.add(parseTokens.get(j));
                    }
                    ASTNode childNode = new AlterDropConstraintNode(tokens);
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else {
                    try {
                        throw new ParseFailedException("Parse failed:There exists syntex error in the input sql!");
                    }
                    catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("MODIFY")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                AlterModifyColumnNode childNode = new AlterModifyColumnNode();
                for (int j = i + 1; j < parseTokens.size(); j++) {
                    if (j == i + 1) {
                        childNode.setName(parseTokens.get(j));
                    }
                    if (j == i + 2) {
                        childNode.setType(parseTokens.get(j));
                    }
                    if (j > i + 3) {
                        try {
                            throw new ParseFailedException("Parse failed:There exists syntex error in the input sql!");
                        }
                        catch (ParseFailedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equalsIgnoreCase(";")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                childNode.setTokens(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("RENAME")) {
                if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("COLUMN")) {
                    tokens = new ArrayList<>();
                    tokens.add(parseTokens.get(i));
                    for (int j = i + 1; j < parseTokens.size(); j++) {
                        if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equalsIgnoreCase(";")) {
                            i = j - 1;
                            break;
                        }
                        tokens.add(parseTokens.get(j));
                    }
                    ASTNode childNode = new AlterRenameColumnNode(tokens);
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("TO")) {
                    tokens = new ArrayList<>();
                    tokens.add(parseTokens.get(i));
                    for (int j = i + 1; j < parseTokens.size(); j++) {
                        if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equalsIgnoreCase(";")) {
                            i = j - 1;
                            break;
                        }
                        tokens.add(parseTokens.get(j));
                    }
                    ASTNode childNode = new AlterRenameTableNode(tokens);
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                }
                else {
                    try {
                        throw new ParseFailedException("Parse failed:There exists syntex error in the input sql!");
                    }
                    catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new AlterEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Parse failed:" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }
        return root;
    }

    /**
     * Create view
     * Grammar: CREATE [OR REPLACE] VIEW view_name [(column_name [, column_name]...)] AS SELECT_statement;
     * Example: CREATE OR REPLACE VIEW employee_details (full_name, pay)
     *          AS SELECT first_name, last_name, salary FROM employees;
     */
    private ASTNode parseCreateView(List<Token> parseTokens) {
        List<Token> tokens = new ArrayList<>();
        ASTNode root = new ViewCreateNode();
        ASTNode currentNode = root;
        for (int i = 0; i < parseTokens.size(); i++) {
            if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("CREATE")) {
                tokens.add(parseTokens.get(i));
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("OR")) {
                tokens.add(parseTokens.get(i));
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("REPLACE")) {
                tokens.add(parseTokens.get(i));
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("VIEW")) {
                tokens.add(parseTokens.get(i));
                root.setTokens(tokens);
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER) && currentNode instanceof ViewCreateNode) {
                tokens = new ArrayList<>();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("AS")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(parseTokens.get(j));
                }
                ASTNode childNode = new ViewNameNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("AS")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new ViewTargetNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
                tokens = parseTokens.subList(i + 1, parseTokens.size());
                ASTNode selectRootNode = parseSelect(tokens);
                currentNode.addChild(selectRootNode);
                currentNode = selectRootNode.getDeepestChild();
                ASTNode viewEndNode = new ViewEndNode();
                currentNode.addChild(viewEndNode);
                currentNode = viewEndNode;
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Failed to parse:" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

    /**
     * Create PROCEDURE
     * Grammar: CREATE [OR REPLACE] PROCEDURE procedure_name ([parameter_list])
     * IS [LOCAL DECLARATIONS]
     * BEGIN
     *     -- PL/SQL statements
     * [EXCEPTION]
     *     -- Exception handling
     * END [procedure_name];
     * Example: CREATE OR REPLACE PROCEDURE update_salary(
     *              employee_id IN NUMBER,
     *              new_salary IN OUT NUMBER
     *          ) IS
     *          BEGIN
     *              IF new_salary < 3000 THEN
     *                  new_salary := new_salary * 1.1;
     *              ELSE
     *                  new_salary := new_salary * 1.05;
     *              END IF;
     *          END update_salary;
     *          /
     */
    private ASTNode parseProcedure(List<Token> parseTokens) {
        ASTNode root = new ProcedureNode(new ArrayList<>());
        ASTNode currentNode = root;
        for (int i = 0; i < parseTokens.size(); i++) {
            // match CREATE PROCEDURE
            if (root.getTokens().isEmpty()) {
                for (int j = i; j < parseTokens.size(); j++) {
                    root.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("PROCEDURE")) {
                        i = j;
                        break;
                    }
                }
            }
            // match procedure name
            else if (currentNode == root && parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                ASTNode childNode = new ProcedureObjNode();
                childNode.addToken(parseTokens.get(i));
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match parameter list
            else if (currentNode instanceof ProcedureObjNode && parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals("(")) {
                i++;
                for (int ii = i; ii < parseTokens.size(); ii++) {
                    if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("IS")) {
                        i = ii - 1;
                        break;
                    }
                    List<Token> tokens = new ArrayList<>();
                    ProcedureColumnNode child = new ProcedureColumnNode();
                    child.setName(parseTokens.get(ii));
                    tokens.add(parseTokens.get(ii));
                    List<Token> constraint = new ArrayList<>();
                    boolean typeMatch = false;
                    int typeIdx = -1;
                    for (int j = ii + 1; j < parseTokens.size(); j++) {
                        if (!typeMatch && !(parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))))
                        {
                            child.setType(parseTokens.get(j));
                            typeMatch = true;
                            typeIdx = j;
                        }
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))) {
                            child.addInOut(parseTokens.get(j));
                        }
                        // Check () or REFERENCES other_table(other_column)
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) &&
                                (parseTokens.get(j).getValue().equalsIgnoreCase("REFERENCES")
                                        || parseTokens.get(j).getValue().equalsIgnoreCase("CHECK"))) {
                            tokens.add(parseTokens.get(j));
                            if (!(parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))))
                                constraint.add(parseTokens.get(j));
                            Stack<String> stack = new Stack<>();
                            for (int k = j + 1; k < parseTokens.size(); k++) {
                                tokens.add(parseTokens.get(k));
                                if (!(parseTokens.get(k).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(k).getValue().equalsIgnoreCase("IN") || parseTokens.get(k).getValue().equalsIgnoreCase("OUT"))))
                                    constraint.add(parseTokens.get(k));
                                if (parseTokens.get(k).getValue().equals("(")) {
                                    stack.push("(");
                                    for (int t = k + 1; t < parseTokens.size(); t++) {
                                        tokens.add(parseTokens.get(t));
                                        if (!(parseTokens.get(t).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(t).getValue().equalsIgnoreCase("IN") || parseTokens.get(t).getValue().equalsIgnoreCase("OUT"))))
                                            constraint.add(parseTokens.get(t));
                                        if (parseTokens.get(t).getValue().equals("(")) {
                                            stack.push("(");
                                        } else if (parseTokens.get(t).getValue().equals(")")) {
                                            stack.pop();
                                            if (stack.empty()) {
                                                ii = t;
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        if ((parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(",")) ||
                                (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(")"))) {
                            ii = j;
                            break;
                        }
                        tokens.add(parseTokens.get(j));
                        if (typeMatch && j != typeIdx) {
                            if (!(parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))))
                                constraint.add(parseTokens.get(j));
                        }
                    }
                    child.setTokens(tokens);
                    child.setConstraint(constraint);
                    currentNode.addChild(child);
                    currentNode = child;
                }
            }
            // match procedure return definition
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("IS")) {
                ASTNode childNode = new ProcedureRetDefNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("BEGIN")) {
                        i = j - 1;
                        break;
                    }
                    childNode.addToken(parseTokens.get(j));
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match procedure begin
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("BEGIN")) {
                ASTNode childNode = new ProcedureBeginNode();
                childNode.addToken(parseTokens.get(i));
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match procedure body
            else if (currentNode instanceof ProcedureBeginNode) {
                for (int ii = i; ii < parseTokens.size(); ii++) {
                    if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("END")) {
                        i = ii - 1;
                        break;
                    }
                    else if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("EXCEPTION")) {
                        i = ii - 1;
                        break;
                    }
                    else if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("IF")) {
                        int index = -1;
                        for (int j = ii; j < parseTokens.size(); j++) {
                            if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("END")
                            && j + 1 < parseTokens.size() && parseTokens.get(j + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j + 1).getValue().equalsIgnoreCase("IF")
                                    && j + 2 < parseTokens.size() && parseTokens.get(j + 2).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j + 2).getValue().equals(";") ) {
                                index = j + 2;
                                break;
                            }
                        }
                        if (index == -1) {
                            try {
                                throw new ParseFailedException("There exists syntax error and no END if block is found!");
                            } catch (ParseFailedException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            ASTNode childNode = parseIFELSE(parseTokens.subList(ii, index + 1));
                            currentNode.addChild(childNode);
                            currentNode = childNode.getDeepestChild();
                            ii = index;
                        }
                    }
                    else {
                        ASTNode childNode = new ProcedurePLStatementNode();
                        for (int j = ii; j < parseTokens.size(); j++) {
                            childNode.addToken(parseTokens.get(j));
                            if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                                ii = j;
                                break;
                            }
                        }
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                    }
                }
            }
            // match exception
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("EXCEPTION")) {
                int index = -1;
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("END")) {
                        index = j;
                        break;
                    }
                }
                if (index == -1) {
                    try {
                        throw new ParseFailedException("There exists syntax error and no END block is found!");
                    } catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }
                ASTNode childNode = parseException(parseTokens.subList(i, index + 1));
                currentNode.addChild(childNode);
                currentNode = childNode.getDeepestChild();
                childNode = new ProcedureEndNode();
                for (int j = index; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
                break;
            }
            // match procedure end
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) {
                ASTNode childNode = new ProcedureEndNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
                break;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Failed to parse:" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }

        }

        return root;
    }

    /**
     * Create FUNCTION
     * Grammar: CREATE OR REPLACE FUNCTION function_name (
     *     parameter1 [IN | OUT] datatype1 ,
     *     parameter2 [IN | OUT] datatype2 ,
     *     ...
     * ) RETURN return_datatype IS
     * BEGIN
     *     -- function body
     *     RETURN result;
     * END;
     * /
     * Example: CREATE OR REPLACE FUNCTION string_length (
     *          input_string VARCHAR2
     *          ) RETURN NUMBER IS
     *          BEGIN
     *          RETURN LENGTH(input_string);
     *          END;
     *          /
     */
    private ASTNode parseFunction(List<Token> parseTokens) {
        ASTNode root = new FunctionNode();
        ASTNode currentNode = root;
        for (int i = 0; i < parseTokens.size(); i++) {
            // CREATE [OR REPLACE] FUNCTION
            if (root.getTokens().isEmpty()) {
                for (int j = i; j < parseTokens.size(); j++) {
                    root.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("FUNCTION")) {
                        i = j;
                        break;
                    }
                }
            }
            // match function name
            else if (currentNode == root && parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                ASTNode childNode = new FunctionNameNode();
                childNode.addToken(parseTokens.get(i));
                currentNode.addChild(childNode);
                currentNode = childNode;
                i++;
            }
            // match parameters
            else if (currentNode instanceof FunctionNameNode && parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                for (int ii = i; ii < parseTokens.size(); ii++) {
                    if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("RETURN")) {
                        i = ii - 1;
                        break;
                    }
                    List<Token> tokens = new ArrayList<>();
                    FunctionColumnNode child = new FunctionColumnNode();
                    child.setName(parseTokens.get(ii));
                    tokens.add(parseTokens.get(ii));
                    List<Token> constraint = new ArrayList<>();
                    boolean typeMatch = false;
                    int typeIdx = -1;
                    for (int j = ii + 1; j < parseTokens.size(); j++) {
                        if (!typeMatch && !(parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))))
                        {
                            child.setType(parseTokens.get(j));
                            typeMatch = true;
                            typeIdx = j;
                        }
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))) {
                            child.addInOut(parseTokens.get(j));
                        }
                        // Check () or REFERENCES other_table(other_column)
                        if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) &&
                                (parseTokens.get(j).getValue().equalsIgnoreCase("REFERENCES")
                                        || parseTokens.get(j).getValue().equalsIgnoreCase("CHECK"))) {
                            tokens.add(parseTokens.get(j));
                            if (!(parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))))
                                constraint.add(parseTokens.get(j));
                            Stack<String> stack = new Stack<>();
                            for (int k = j + 1; k < parseTokens.size(); k++) {
                                tokens.add(parseTokens.get(k));
                                if (!(parseTokens.get(k).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(k).getValue().equalsIgnoreCase("IN") || parseTokens.get(k).getValue().equalsIgnoreCase("OUT"))))
                                    constraint.add(parseTokens.get(k));
                                if (parseTokens.get(k).getValue().equals("(")) {
                                    stack.push("(");
                                    for (int t = k + 1; t < parseTokens.size(); t++) {
                                        tokens.add(parseTokens.get(t));
                                        if (!(parseTokens.get(t).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(t).getValue().equalsIgnoreCase("IN") || parseTokens.get(t).getValue().equalsIgnoreCase("OUT"))))
                                            constraint.add(parseTokens.get(t));
                                        if (parseTokens.get(t).getValue().equals("(")) {
                                            stack.push("(");
                                        } else if (parseTokens.get(t).getValue().equals(")")) {
                                            stack.pop();
                                            if (stack.empty()) {
                                                ii = t;
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        if ((parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(",")) ||
                                (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(")"))) {
                            ii = j;
                            break;
                        }
                        tokens.add(parseTokens.get(j));
                        if (typeMatch && j != typeIdx) {
                            if (!(parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && (parseTokens.get(j).getValue().equalsIgnoreCase("IN") || parseTokens.get(j).getValue().equalsIgnoreCase("OUT"))))
                                constraint.add(parseTokens.get(j));
                        }
                    }
                    child.setTokens(tokens);
                    child.setConstraint(constraint);
                    currentNode.addChild(child);
                    currentNode = child;
                }
            }
            // match return definition
            else if (currentNode instanceof FunctionColumnNode && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("RETURN")) {
                FunctionRetDefNode childNode = new FunctionRetDefNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("RETURN")) {
                        childNode.addToken(parseTokens.get(j));
                    }
                    else if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equals("IS")) {
                        childNode.addToken(parseTokens.get(j));
                        i = j;
                        break;
                    }
                    else {
                        childNode.addToken(parseTokens.get(j));
                        childNode.setType(parseTokens.get(j));
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match function begin
            else if (currentNode instanceof FunctionRetDefNode && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("BEGIN")) {
                ASTNode childNode = new FunctionBeginNode();
                childNode.addToken(parseTokens.get(i));
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match function body
            else if (currentNode instanceof FunctionBeginNode) {
                for (int ii = i; ii < parseTokens.size(); ii++) {
                    if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("END")) {
                        i = ii - 1;
                        break;
                    }
                    else if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("EXCEPTION")) {
                        i = ii - 1;
                        break;
                    }
                    else if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("IF")) {
                        int index = -1;
                        for (int j = ii; j < parseTokens.size(); j++) {
                            if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("END")
                                    && j + 1 < parseTokens.size() && parseTokens.get(j + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j + 1).getValue().equalsIgnoreCase("IF")
                                    && j + 2 < parseTokens.size() && parseTokens.get(j + 2).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j + 2).getValue().equals(";") ) {
                                index = j + 2;
                                break;
                            }
                        }
                        if (index == -1) {
                            try {
                                throw new ParseFailedException("There exists syntax error and no END if block is found!");
                            } catch (ParseFailedException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            ASTNode childNode = parseIFELSE(parseTokens.subList(ii, index + 1));
                            currentNode.addChild(childNode);
                            currentNode = childNode.getDeepestChild();
                            ii = index;
                        }
                    }
                    else if (parseTokens.get(ii).hasType(Token.TokenType.KEYWORD) && parseTokens.get(ii).getValue().equalsIgnoreCase("RETURN")) {
                        ASTNode childNode = new FunctionReturnNode();
                        for (int j = ii; j < parseTokens.size(); j++) {
                            childNode.addToken(parseTokens.get(j));
                            if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                                ii = j;
                                break;
                            }
                        }
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                    }
                    else {
                        ASTNode childNode = new FunctionBodyNode();
                        for (int j = ii; j < parseTokens.size(); j++) {
                            childNode.addToken(parseTokens.get(j));
                            if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                                ii = j;
                                break;
                            }
                        }
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                    }
                }
            }
            // match function end
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) {
                ASTNode childNode = new FunctionEndNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    childNode.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.SYMBOL) && parseTokens.get(j).getValue().equals(";")) {
                        i = j;
                        break;
                    }
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
                break;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Failed to parse:" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }

        }

        return root;
    }

    /**
     * Create TRIGGER
     * Grammar: CREATE OR REPLACE TRIGGER trigger_name
     * BEFORE | AFTER | INSTEAD OF -- OpenGauss does not support INSTEAD OF
     * { INSERT | UPDATE | DELETE }
     * ON table_name
     * [REFERENCING NEW AS new OLD AS old] -- OpenGauss does not support this
     * FOR EACH ROW
     * [WHEN (condition)]
     * BEGIN
     *     -- trigger body
     * END;
     * Example: CREATE OR REPLACE TRIGGER log_insert
     *          BEFORE INSERT ON employees
     *          FOR EACH ROW
     *          BEGIN
     *              INSERT INTO audit_log (action, employee_id) VALUES ('INSERT', :NEW.id);
     *          END;
     */
    private ASTNode parseTrigger(List<Token> parseTokens) {
        ASTNode root = new TriggerNode();
        ASTNode currentNode = root;
        for (int i = 0; i < parseTokens.size(); i++) {
            // match create trigger
            if (root.getTokens().isEmpty()) {
                for (int j = i; j < parseTokens.size(); j++) {
                    root.addToken(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("TRIGGER")) {
                        i = j;
                        break;
                    }
                }
            }
            // match trigger name
            else if (currentNode == root && parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                ASTNode childNode = new TriggerNameNode();
                childNode.addToken(parseTokens.get(i));
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match trigger condition
            else if (currentNode instanceof TriggerNameNode && (
                    parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("BEFORE") ||
                            parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("AFTER") ||
                            parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("INSTEAD OF")
                    )) {
                if (i + 1 < parseTokens.size() && (
                        parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("INSERT") ||
                                parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("UPDATE") ||
                                parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("DELETE")
                        )) {
                    TriggerConditionNode childNode = new TriggerConditionNode();
                    childNode.setCondition(parseTokens.get(i));
                    childNode.setAction(parseTokens.get(i + 1));
                }
                else {
                    try {
                        throw new ParseFailedException("There exists syntax error!");
                    } catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // match trigger on table
            else if (currentNode instanceof TriggerConditionNode && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ON")) {
                if (i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.IDENTIFIER)) {
                    ASTNode childNode = new TriggerObjNode();
                    childNode.addToken(parseTokens.get(i));
                    childNode.addToken(parseTokens.get(i + 1));
                    currentNode.addChild(childNode);
                    currentNode = childNode;
                    i++;
                }
                else {
                    try {
                        throw new ParseFailedException("There exists syntax error!");
                    } catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // match [REFERENCING NEW AS new OLD AS old]
            else if (currentNode instanceof TriggerObjNode && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("REFERENCING")) {
                ASTNode childNode = new TriggerOptionNode();
                for (int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("FOR")) {
                        i = j - 1;
                        break;
                    }
                    childNode.addToken(parseTokens.get(j));
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match FOR EACH ROW
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("FOR")) {
                if ((i + 1 < parseTokens.size() && parseTokens.get(i + 1).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 1).getValue().equalsIgnoreCase("EACH"))
                    && (i + 2 < parseTokens.size() && parseTokens.get(i + 2).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i + 2).getValue().equalsIgnoreCase("ROW"))) {
                        ASTNode childNode = new TriggerForEachRowNode();
                        childNode.addToken(parseTokens.get(i));
                        childNode.addToken(parseTokens.get(i + 1));
                        childNode.addToken(parseTokens.get(i + 2));
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                    }
                else {
                    try {
                            throw new ParseFailedException("There exists syntax error!");
                    } catch (ParseFailedException e) {
                            e.printStackTrace();
                    }
                }
            }
            // match when
            else if (currentNode instanceof TriggerForEachRowNode && parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("WHEN")) {
                ASTNode childNode = new TriggerWhenNode();
                for(int j = i; j < parseTokens.size(); j++) {
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("BEGIN")) {
                        i = j - 1;
                        break;
                    }
                    childNode.addToken(parseTokens.get(j));
                }
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match begin
            else if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("BEGIN")) {
                ASTNode childNode = new TriggerBeginNode();
                childNode.addToken(parseTokens.get(i));
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match trigger body
            else if (currentNode instanceof TriggerBeginNode) {
                //TODO: parse trigger body
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
                try {
                    throw new ParseFailedException("Failed to parse:" + parseTokens.get(i));
                }
                catch (ParseFailedException e) {
                    e.printStackTrace();
                }
            }
        }

        return root;
    }

}
