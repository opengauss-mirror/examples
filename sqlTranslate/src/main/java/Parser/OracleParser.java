package Parser;

import Lexer.OracleLexer;
import Lexer.Token;
import Parser.AST.ASTNode;
import Exception.ParseFailedException;
import Parser.AST.CaseWhen.CaseConditionNode;
import Parser.AST.CaseWhen.CaseElseNode;
import Parser.AST.CaseWhen.CaseEndNode;
import Parser.AST.CaseWhen.CaseThenNode;
import Parser.AST.CreateTable.*;
import Parser.AST.DropTable.DropTableEndNode;
import Parser.AST.DropTable.DropTableNameNode;
import Parser.AST.DropTable.DropTableNode;
import Parser.AST.DropTable.DropTableOptionNode;
import Parser.AST.Insert.InsertDataNode;
import Parser.AST.Insert.InsertEndNode;
import Parser.AST.Insert.InsertNode;
import Parser.AST.Insert.InsertObjNode;
import Parser.AST.Join.*;
import Parser.AST.Select.*;

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
        // check if the input is a create table statement
        if ((lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(1).getValue().equalsIgnoreCase("TABLE")) ||
                (lexer.getTokens().get(0).getValue().equalsIgnoreCase("CREATE") && lexer.getTokens().get(2).getValue().equalsIgnoreCase("TEMPORARY") && lexer.getTokens().get(3).getValue().equalsIgnoreCase("TABLE")) ) {
            return parseCreateTab(lexer.getTokens());
        }
        else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("INSERT")) {
            return parseInsert(lexer.getTokens());
        }
        else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("DROP")) {
            return parseDrop(lexer.getTokens());
        }
        else if (lexer.getTokens().get(0).getValue().equalsIgnoreCase("SELECT")) {
            return parseSelect(lexer.getTokens());
        }
        else {
            try {
                throw new ParseFailedException("Parse failed!");
            }
            catch (ParseFailedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * CREATE TEMPORARY? TABLE _table ( _column _type [, _column _type]... ) [CONSTRAINT _constraint] [, _column _type [, _column _type]...]
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

            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode EndNode = new CRTEndNode(tokens);
                currentNode.addChild(EndNode);
                currentNode = EndNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
//                System.out.println("Fail to parse:" + parseTokens.get(i).getValue());
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
     * DROP TABLE _table (CASCADE CONSTRAINTS)?
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
        ASTNode root = new DropTableNode(tokens);
        ASTNode currentNode = root;

        for (int i = 2; i < parseTokens.size(); i++) {
            if (parseTokens.get(i).hasType(Token.TokenType.IDENTIFIER)) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new DropTableNameNode(tokens);
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
                ASTNode childNode = new DropTableOptionNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (parseTokens.get(i).hasType(Token.TokenType.SYMBOL) && parseTokens.get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(parseTokens.get(i));
                ASTNode childNode = new DropTableEndNode(tokens);
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
                SelectObjNode childNode = new SelectObjNode();
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
                ASTNode childNode = new SelectObjNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            // match select_tab (possible last token: ; | GROUP BY | ORDER BY | HAVING | WHERE | UNION)
            else if (currentNode instanceof SelectObjNode) {
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
                ASTNode childNode = new SelectTableNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
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
     * CASE WHEN expr THEN expr [ELSE expr] END
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
            tokens.add(parseTokens.get(i));
            if (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("THEN")) {
                currentIndex = i;
                break;
            }
        }
        ASTNode root = new CaseConditionNode(tokens);
        ASTNode currentNode = root;

        tokens = new ArrayList<>();
        for (int i = currentIndex; i < parseTokens.size(); i++) {
            tokens.add(parseTokens.get(i));
            if ( (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("ELSE"))
            || (parseTokens.get(i).hasType(Token.TokenType.KEYWORD) && parseTokens.get(i).getValue().equalsIgnoreCase("END")) ) {
                currentIndex = i;
                break;
            }
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
                        i = j;
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
     * JOIN clause
     * For example: table1 t1 JOIN table2 t2 ON|USING t1.id = t2.id (parseTokens should start with table)
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
            try {
                throw new ParseFailedException("Fail to parse:" + parseTokens.get(index));
            }
            catch (ParseFailedException e) {
                e.printStackTrace();
            }
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
                    try {
                        throw new ParseFailedException("Fail to parse:" + parseTokens.get(index));
                    }
                    catch (ParseFailedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            try {
                throw new ParseFailedException("Fail to parse:" + parseTokens.get(index));
            }
            catch (ParseFailedException e) {
                e.printStackTrace();
            }
        }

        return root;
    }
}
