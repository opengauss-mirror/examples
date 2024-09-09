package Parser;

import Lexer.OracleLexer;
import Lexer.Token;
import Parser.AST.ASTNode;
import Exception.ParseFailedException;
import Parser.AST.CreateTable.*;
import Parser.AST.DropTable.DropTableEndNode;
import Parser.AST.DropTable.DropTableNameNode;
import Parser.AST.DropTable.DropTableNode;
import Parser.AST.DropTable.DropTableOptionNode;
import Parser.AST.Insert.InsertDataNode;
import Parser.AST.Insert.InsertEndNode;
import Parser.AST.Insert.InsertNode;
import Parser.AST.Insert.InsertObjNode;

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
        List<Token> tokens = lexer.getTokens();
        // check if the input is a create table statement
        if ((tokens.get(0).getValue().equalsIgnoreCase("CREATE") && tokens.get(1).getValue().equalsIgnoreCase("TABLE")) ||
                (tokens.get(0).getValue().equalsIgnoreCase("CREATE") && tokens.get(2).getValue().equalsIgnoreCase("TEMPORARY") && tokens.get(3).getValue().equalsIgnoreCase("TABLE")) ) {
            return parseCreateTab();
        }
        else if (tokens.get(0).getValue().equalsIgnoreCase("INSERT")) {
            return parseInsert();
        }
        else if (tokens.get(0).getValue().equalsIgnoreCase("DROP")) {
            return parseDrop();
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

    private ASTNode parseCreateTab() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(lexer.getTokens().get(0));
        ASTNode root = new CreateTabNode(tokens);
        ASTNode currentNode = null;
        for (int i = 1; i < lexer.getTokens().size(); i++) {
//            System.out.println("Parsing the token: " + lexer.getTokens().get(i).getValue());
            if (i == 1 && lexer.getTokens().get(i).getValue().equalsIgnoreCase("TABLE")) {
                tokens = new ArrayList<>();

                tokens.add(lexer.getTokens().get(i));
                try {
                    tokens.add(lexer.getTokens().get(i + 1));
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
            else if (i == 1 && !lexer.getTokens().get(i).getValue().equalsIgnoreCase("TABLE")){
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                for (int j = i + 1; j < lexer.getTokens().size(); j++) {
                    if (lexer.getTokens().get(j).getValue().equalsIgnoreCase("TABLE")) {
                        ASTNode child = new TableTypeNode(tokens);
                        root.addChild(child);
                        currentNode = child;
                        tokens = new ArrayList<>();
                        tokens.add(lexer.getTokens().get(j));
                        try {
                            tokens.add(lexer.getTokens().get(j + 1));
                        }
                        catch (ParseFailedException e) {
                            e.printStackTrace();
                        }
                        i = j + 2; // pass the "("
                        break;
                    }
                    tokens.add(lexer.getTokens().get(j));
                }

                ASTNode child = new TableNode(tokens);
                currentNode.addChild(child);
                currentNode = child;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.IDENTIFIER)) {
                // Token.TokenType.IDENTIFIER ... , -> column node
                tokens = new ArrayList<>();
                ColumnNode child = new ColumnNode();
                child.setName(lexer.getTokens().get(i));
                tokens.add(lexer.getTokens().get(i));
                List <Token> constraint = new ArrayList<>();
                for (int j = i + 1; j < lexer.getTokens().size(); j++) {
                    if (j == i + 1) {
                        child.setType(lexer.getTokens().get(j));
                    }
                    // Check () or REFERENCES other_table(other_column)
                    if (lexer.getTokens().get(j).hasType(Token.TokenType.KEYWORD) &&
                            (lexer.getTokens().get(j).getValue().equalsIgnoreCase("REFERENCES")
                                    || lexer.getTokens().get(j).getValue().equalsIgnoreCase("CHECK"))) {
                        tokens.add(lexer.getTokens().get(j));
                        Stack<String> stack = new Stack<>();
                        for (int k = j + 1; k < lexer.getTokens().size(); k++) {
                            tokens.add(lexer.getTokens().get(k));
                            constraint.add(lexer.getTokens().get(k));
                            if (lexer.getTokens().get(k).getValue().equals("(")) {
                                stack.push("(");
                                for (int t = k + 1; t < lexer.getTokens().size(); t++) {
                                    tokens.add(lexer.getTokens().get(t));
                                    constraint.add(lexer.getTokens().get(t));
                                    if (lexer.getTokens().get(t).getValue().equals("(")) {
                                        stack.push("(");
                                    }
                                    else if (lexer.getTokens().get(t).getValue().equals(")")) {
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
                    if ((lexer.getTokens().get(j).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(j).getValue().equals(",")) ||
                            (lexer.getTokens().get(j).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(j).getValue().equals(")")) ) {
                        i = j;
                        break;
                    }
                    tokens.add(lexer.getTokens().get(j));
                    if (j != i + 1) {
                        constraint.add(lexer.getTokens().get(j));
                    }
                }
                child.setTokens(tokens);
                child.setConstraint(constraint);
                currentNode.addChild(child);
                currentNode = child;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.KEYWORD)
                    && lexer.getTokens().get(i).getValue().equalsIgnoreCase("CONSTRAINT")) {
                // Table constraint
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                for (int j = i + 1; j < lexer.getTokens().size(); j++) {
                    /**
                     * CONSTRAINT pk_example PRIMARY KEY (column1, column2)
                     * CONSTRAINT uk_example UNIQUE (column1)
                     * CONSTRAINT fk_example FOREIGN KEY (column1) REFERENCES other_table(column2)
                     * CONSTRAINT chk_example CHECK (column1 > 0)
                     */
                    if (lexer.getTokens().get(j).hasType(Token.TokenType.KEYWORD) &&
                            (lexer.getTokens().get(j).getValue().equalsIgnoreCase("UNIQUE")
                                    || lexer.getTokens().get(j).getValue().equalsIgnoreCase("CHECK")
                                    || lexer.getTokens().get(j).getValue().equalsIgnoreCase("PRIMARY KEY")
                                    || lexer.getTokens().get(j).getValue().equalsIgnoreCase("FOREIGN KEY")
                                    || lexer.getTokens().get(j).getValue().equalsIgnoreCase("REFERENCES"))) {
                        tokens.add(lexer.getTokens().get(j));
                        Stack<String> stack = new Stack<>();
                        for (int k = j + 1; k < lexer.getTokens().size(); k++) {
                            tokens.add(lexer.getTokens().get(k));
                            if (lexer.getTokens().get(k).getValue().equals("(")) {
                                stack.push("(");
                                for (int t = k + 1; t < lexer.getTokens().size(); t++) {
                                    tokens.add(lexer.getTokens().get(t));
                                    if (lexer.getTokens().get(t).getValue().equals("(")) {
                                        stack.push("(");
                                    }
                                    else if (lexer.getTokens().get(t).getValue().equals(")")) {
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
                    if ((lexer.getTokens().get(j).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(j).getValue().equals(",")) ||
                            (lexer.getTokens().get(j).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(j).getValue().equals(")")) ) {
                        i = j;
                        break;
                    }
                    tokens.add(lexer.getTokens().get(j));
                }

            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                ASTNode EndNode = new CRTEndNode(tokens);
                currentNode.addChild(EndNode);
                currentNode = EndNode;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.EOF)) {
                break;
            }
            else {
//                System.out.println("Fail to parse:" + lexer.getTokens().get(i).getValue());
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

    private ASTNode parseInsert() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(lexer.getTokens().get(0));
        try {
            tokens.add(lexer.getTokens().get(1));
        }
        catch (ParseFailedException e) {
            e.printStackTrace();
        }
        ASTNode root = new InsertNode(tokens);
        ASTNode currentNode = root;

        for (int i = 2; i < lexer.getTokens().size(); i++) {
            if (lexer.getTokens().get(i).hasType(Token.TokenType.IDENTIFIER)) { // table name
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                if (i + 1 < lexer.getTokens().size() && ! (lexer.getTokens().get(i + 1).hasType(Token.TokenType.KEYWORD) && lexer.getTokens().get(i + 1).getValue().equalsIgnoreCase("VALUES"))) {
                    for (int j = i + 1; j < lexer.getTokens().size(); j++) {
                        tokens.add(lexer.getTokens().get(j));
                        if (lexer.getTokens().get(j).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(j).getValue().equals(")")) {
                            i = j;
                            break;
                        }
                    }
                }
                ASTNode childNode = new InsertObjNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.KEYWORD) && lexer.getTokens().get(i).getValue().equalsIgnoreCase("VALUES")) {
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                for (int j = i + 1; j < lexer.getTokens().size(); j++) {
                    if (lexer.getTokens().get(j).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(j).getValue().equals(";")) {
                        i = j - 1;
                        break;
                    }
                    tokens.add(lexer.getTokens().get(j));
                }
                ASTNode childNode = new InsertDataNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                ASTNode childNode = new InsertEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.EOF)) {
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

    // Drop table
    private ASTNode parseDrop() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(lexer.getTokens().get(0));
        try {
            tokens.add(lexer.getTokens().get(1));
        }
        catch (ParseFailedException e) {
            e.printStackTrace();
        }
        ASTNode root = new DropTableNode(tokens);
        ASTNode currentNode = root;

        for (int i = 2; i < lexer.getTokens().size(); i++) {
            if (lexer.getTokens().get(i).hasType(Token.TokenType.IDENTIFIER)) {
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                ASTNode childNode = new DropTableNameNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.KEYWORD) && lexer.getTokens().get(i).getValue().equalsIgnoreCase("CASCADE")) {
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                if (i + 1 < lexer.getTokens().size() && (lexer.getTokens().get(i + 1).hasType(Token.TokenType.KEYWORD) && lexer.getTokens().get(i + 1).getValue().equals("CONSTRAINTS"))) {
                    tokens.add(lexer.getTokens().get(i + 1));
                    i++;
                }
                ASTNode childNode = new DropTableOptionNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.SYMBOL) && lexer.getTokens().get(i).getValue().equals(";")) {
                tokens = new ArrayList<>();
                tokens.add(lexer.getTokens().get(i));
                ASTNode childNode = new DropTableEndNode(tokens);
                currentNode.addChild(childNode);
                currentNode = childNode;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.EOF)) {
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
