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
import Parser.AST.Select.SelectNode;
import Parser.AST.Select.SelectObjNode;

import java.util.Stack;

import java.util.ArrayList;
import java.util.Arrays;
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
                for (int j = i + 1; j < parseTokens.size(); j++) {
                    //TODO: match select_obj and reindex i
                }
            }
        }

        return root;
    }

    /**
     * CASE WHEN expr THEN expr [ELSE expr] END
     */
    private ASTNode parseCaseWhen(List<Token> parseTokens) {
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
                    tokens.add(parseTokens.get(j));
                    if (parseTokens.get(j).hasType(Token.TokenType.KEYWORD) && parseTokens.get(j).getValue().equalsIgnoreCase("END")) {
                        childNode = new CaseElseNode(tokens);
                        currentNode.addChild(childNode);
                        currentNode = childNode;
                        i = j;
                        break;
                    }
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


}
