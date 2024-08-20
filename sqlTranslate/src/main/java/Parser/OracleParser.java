package Parser;

import Lexer.OracleLexer;
import Lexer.Token;
import Parser.AST.ASTNode;
import Exception.ParseFailedException;
import Parser.AST.CreateTable.CreateTabNode;
import Parser.AST.CreateTable.TableNode;
import Parser.AST.CreateTable.TableTypeNode;

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
        }
        return root;
    }


}
