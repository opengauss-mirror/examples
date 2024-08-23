package Parser;

import Lexer.OracleLexer;
import Lexer.Token;
import Parser.AST.ASTNode;
import Exception.ParseFailedException;
import Parser.AST.CreateTable.ColumnNode;
import Parser.AST.CreateTable.CreateTabNode;
import Parser.AST.CreateTable.TableNode;
import Parser.AST.CreateTable.TableTypeNode;
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
        else {
            throw new ParseFailedException("Parse failed!");
        }
    }

    private ASTNode parseCreateTab() {
        List <Token> tokens = new ArrayList<>();
        tokens.add(lexer.getTokens().get(0));
        ASTNode root = new CreateTabNode(tokens);
        ASTNode currentNode = root;
        for (int i = 1; i < lexer.getTokens().size(); i++) {
            if (i == 1 && lexer.getTokens().get(i).getValue().equalsIgnoreCase("TABLE")) {
                tokens.clear();
                tokens.add(lexer.getTokens().get(i));
                try {
                    tokens.add(lexer.getTokens().get(i + 1));
                }
                catch (ParseFailedException e) {
                    throw new ParseFailedException("Parse failed!");
                }
                i++;
                ASTNode child = new TableNode(tokens);
                root.addChild(child);
                currentNode = child;
            }
            else if (i == 1 && !lexer.getTokens().get(i).getValue().equalsIgnoreCase("TABLE")){
                tokens.clear();
                tokens.add(lexer.getTokens().get(i));
                for (int j = i + 1; j < lexer.getTokens().size(); j++) {
                    if (lexer.getTokens().get(j).getValue().equalsIgnoreCase("TABLE")) {
                        ASTNode child = new TableTypeNode(tokens);
                        root.addChild(child);
                        currentNode = child;
                        tokens.clear();
                        tokens.add(lexer.getTokens().get(j));
                        try {
                            tokens.add(lexer.getTokens().get(j + 1));
                        }
                        catch (ParseFailedException e) {
                            throw new ParseFailedException("Parse failed!");
                        }
                        i = j + 1;
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
                tokens.clear();
                ColumnNode child = new ColumnNode();
                child.setName(lexer.getTokens().get(i));
                tokens.add(lexer.getTokens().get(i));
                List <Token> constraint = new ArrayList<>();
                for (int j = i + 1; j < lexer.getTokens().size(); j++) {
                    if (j == i + 1) {
                        child.setType(lexer.getTokens().get(j));
                    }
                    else {
                        constraint.add(lexer.getTokens().get(j));
                    }
                    tokens.add(lexer.getTokens().get(j));
                    // Check () or REFERENCES other_table(other_column)
                    if (lexer.getTokens().get(j).hasType(Token.TokenType.KEYWORD) &&
                            (lexer.getTokens().get(j).getValue().equalsIgnoreCase("REFERENCES") || lexer.getTokens().get(j).getValue().equalsIgnoreCase("CHECK"))) {
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
                        child.setConstraint(constraint);
                        break;
                    }
                }
                child.setTokens(tokens);
                currentNode.addChild(child);
                currentNode = child;
            }
            else if (lexer.getTokens().get(i).hasType(Token.TokenType.KEYWORD) && lexer.getTokens().get(i).getValue().equalsIgnoreCase("CONSTRAINT")) {
                // Table constraint
                tokens.clear();
                tokens.add(lexer.getTokens().get(i));

            }
            else {
                throw new ParseFailedException("Parse failed!");
            }
        }
        return root;
    }


}
