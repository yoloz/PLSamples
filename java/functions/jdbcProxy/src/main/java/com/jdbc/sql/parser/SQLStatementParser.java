/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jdbc.sql.parser;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.jdbc.bean.SQLInfo;
import com.jdbc.sql.ast.SQLExpr;
import com.jdbc.sql.ast.SQLOrderBy;
import com.jdbc.sql.ast.SQLStatement;
import com.jdbc.sql.ast.expr.SQLAllColumnExpr;
import com.jdbc.sql.ast.expr.SQLBinaryOpExpr;
import com.jdbc.sql.ast.expr.SQLIdentifierExpr;
import com.jdbc.sql.ast.expr.SQLInSubQueryExpr;
import com.jdbc.sql.ast.expr.SQLPropertyExpr;
import com.jdbc.sql.ast.expr.SQLVariantRefExpr;
import com.jdbc.sql.ast.statement.*;
import com.jdbc.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.jdbc.util.FnvHash;
import org.apache.log4j.Logger;

public class SQLStatementParser extends SQLStatementParsers {

    private final Logger logger = Logger.getLogger(SQLStatementParser.class);

    public SQLStatementParser(String sql) {
        super(sql);
    }

    public SQLStatementParser(String sql, String dbType) {
        super(sql, dbType);
    }

    public SQLStatementParser(SQLExprParser exprParser) {
        super(exprParser);
    }

    protected SQLStatementParser(Lexer lexer, String dbType) {
        super(lexer, dbType);
    }

    public void break2SQLInfo(Map<String, SQLInfo> sMap) {
        if ("select @@session.tx_read_only".equals(lexer.text)
                && lexer.token == Token.SELECT) {
            SQLSelect select = new SQLSelect();
            MySqlSelectQueryBlock queryBlock = new MySqlSelectQueryBlock();
            queryBlock.addSelectItem(new SQLPropertyExpr(new SQLVariantRefExpr("@@session"), "tx_read_only"));
            select.setQuery(queryBlock);

            SQLSelectStatement stmt = new SQLSelectStatement(select);

            //todo

            lexer.reset(29, '\u001A', Token.EOF);
            return;
        }

        for (; ; ) {
            switch (lexer.token) {
                case EOF:
                case END:
                case UNTIL:
                case ELSE:
                case WHEN:
                    if (lexer.isKeepComments() && lexer.hasComment()) lexer.readAndResetComments();
                    return;
                case SEMI: {
                    int line0 = lexer.getLine();
                    lexer.nextToken();
                    int line1 = lexer.getLine();
                    if (lexer.isKeepComments()) {
                        if (line1 - line0 <= 1) lexer.readAndResetComments();
                    }
                    continue;
                }
                case WITH: {
                    SQLSelectStatement stmt = (SQLSelectStatement) parseWith();
                    //todo
                    continue;
                }
                case SELECT: {
                    SQLSelectStatement stmt = (SQLSelectStatement) parseSelect();
                    parseSelectStatement(sMap, stmt.getSelect());
                    continue;
                }
                case UPDATE: {
                    SQLStatement stmt = parseUpdateStatement();
                    //todo
                    continue;
                }
                case CREATE: {
                    SQLStatement stmt = parseCreate();
                    parseCreateStatement(sMap, stmt);
                    continue;
                }
                case INSERT: {
                    SQLStatement stmt = parseInsert();
                    //todo
                    continue;
                }
                case DELETE: {
                    SQLStatement stmt = parseDeleteStatement();
                    //todo
                    continue;
                }
                case EXPLAIN: {
                    SQLStatement stmt = parseExplain();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case SET: {
                    SQLStatement stmt = parseSet();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case ALTER: {
                    SQLAlterTableStatement stmt = (SQLAlterTableStatement) parseAlter();
                    String fn = Objects.toString(stmt.getSchema(), "");
                    SQLInfo info = sMap.getOrDefault(fn, new SQLInfo());
                    info.setFn(fn);
                    String tb = stmt.getTableName();
                    SQLInfo.SInfo sInfo = info.getSl(tb);
                    sInfo.addOperates("alter");
                    if (!sMap.containsKey(fn)) sMap.put(fn, info);
                    continue;
                }
                case TRUNCATE: {
                    SQLStatement stmt = parseTruncate();
                    //todo
                    continue;
                }
                case USE: {
                    SQLStatement stmt = parseUse();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case GRANT: {
                    SQLStatement stmt = parseGrant();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case REVOKE: {
                    SQLStatement stmt = parseRevoke();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case SHOW: {
                    SQLStatement stmt = parseShow();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case MERGE: {
                    SQLStatement stmt = parseMerge();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case REPEAT: {
                    SQLStatement stmt = parseRepeat();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case DECLARE: {
                    SQLStatement stmt = parseDeclare();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case WHILE: {
                    SQLStatement stmt = parseWhile();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case IF: {
                    SQLStatement stmt = parseIf();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case CASE: {
                    SQLStatement stmt = parseCase();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case OPEN: {
                    SQLStatement stmt = parseOpen();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case FETCH: {
                    SQLStatement stmt = parseFetch();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case DROP: {
                    SQLStatement stmt = parseDrop();
                    //todo
                    continue;
                }
                case COMMENT: {
//                    if (JdbcConstants.MYSQL.equals(this.dbType)) {//mysql 关键字 comment 没有这个语法，oracle才有
//                        return (List<SQLInfo>) sMap.values();
//                    }
                    SQLStatement stmt = parseComment();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case KILL: {
                    SQLStatement stmt = parseKill();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case CLOSE: {
                    SQLStatement stmt = parseClose();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case RETURN: {
                    SQLStatement stmt = parseReturn();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case UPSERT: {
                    SQLStatement stmt = parseUpsert();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case LEAVE: {
                    SQLStatement stmt = parseLeave();
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                default:
                    break;
            }

            if (lexer.token == Token.LBRACE || lexer.identifierEquals("CALL")) {
                SQLCallStatement stmt = parseCall();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }


            if (lexer.identifierEquals("UPSERT")) {
                SQLStatement stmt = parseUpsert();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.identifierEquals("RENAME")) {
                SQLStatement stmt = parseRename();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.identifierEquals(FnvHash.Constants.RELEASE)) {
                SQLStatement stmt = parseReleaseSavePoint();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.identifierEquals(FnvHash.Constants.SAVEPOINT)) {
                SQLStatement stmt = parseSavePoint();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.identifierEquals(FnvHash.Constants.ROLLBACK)) {
                SQLRollbackStatement stmt = parseRollback();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.identifierEquals(FnvHash.Constants.MERGE)) {
                SQLStatement stmt = parseMerge();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.identifierEquals("DUMP")) {
                SQLStatement stmt = parseDump();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.identifierEquals(FnvHash.Constants.COMMIT)) {
                SQLStatement stmt = parseCommit();

                logger.info("statement[" + stmt.getClass() + "] todo authentication");

                continue;
            }

            if (lexer.identifierEquals("RETURN")) {
                SQLStatement stmt = parseReturn();
                logger.info("statement[" + stmt.getClass() + "] todo authentication");
                continue;
            }

            if (lexer.token == Token.LPAREN) {
                char markChar = lexer.current();
                int markBp = lexer.bp();

                do {
                    lexer.nextToken();
                } while (lexer.token == Token.LPAREN);

                if (lexer.token == Token.SELECT) {
                    lexer.reset(markBp, markChar, Token.LPAREN);
                    SQLStatement stmt = parseSelect();
                    //todo
                    continue;
                } else {
                    throw new ParserException("TODO " + lexer.info());
                }
            }
            // throw new ParserException("syntax error, " + lexer.token + " "
            // + lexer.stringVal() + ", pos "
            // + lexer.pos());
            //throw new ParserException("not supported." + lexer.info());
            printError(lexer.token);
        }
    }

    private void parseCreateStatement(Map<String, SQLInfo> map, SQLStatement stmt) {
        SQLInfo info;
        if (stmt instanceof SQLCreateTableStatement) {
            SQLCreateTableStatement createTableStatement = (SQLCreateTableStatement) stmt;

        } else if (stmt instanceof SQLCreateIndexStatement) {
            SQLCreateIndexStatement createIndexStatement = (SQLCreateIndexStatement) stmt;
            String fn = Objects.toString(createIndexStatement.getSchema(), "");
            info = map.getOrDefault(fn, new SQLInfo());
            info.setFn(fn);
            String sn = createIndexStatement.getTableName();
            SQLInfo.SInfo sInfo = info.getSl(sn);
            sInfo.addOperates("createindex");
            if (!map.containsKey(fn)) map.put(fn, info);
        } else if (stmt instanceof SQLCreateViewStatement) {

        } else if (stmt instanceof SQLCreateDatabaseStatement) {

        } else if (stmt instanceof SQLCreateUserStatement) {

        } else throw new ParserException("statement[" + stmt.getClass() + "] todo...");
    }

    private void parseSelectStatement(Map<String, SQLInfo> map, SQLSelect select) {
        SQLSelectQuery query = select.getQuery();
        if (query != null) {
            if (query instanceof SQLSelectQueryBlock) {
                SQLSelectQueryBlock sq = (SQLSelectQueryBlock) query;
                SQLTableSource tb = sq.getFrom();
                if (tb != null) {
                    String fn;
                    SQLInfo sqlInfo;
                    SQLInfo.SInfo sInfo;
                    if (tb instanceof SQLExprTableSource) {
                        String alias = tb.getAlias();
                        SQLExprTableSource exprTableSource = (SQLExprTableSource) tb;
                        fn = Objects.toString(exprTableSource.getSchema(), "");
                        sqlInfo = map.getOrDefault(fn, new SQLInfo());
                        sqlInfo.setFn(fn);
                        SQLExpr expr = ((SQLExprTableSource) tb).getExpr();
                        if (expr instanceof SQLIdentifierExpr) {
                            String sn = ((SQLIdentifierExpr) expr).getName();
                            sInfo = sqlInfo.getSl(sn);
                        } else throw new ParserException("SQLExprTableSource expr[" + expr.getClass() + "] todo...");
                        sInfo.setAlias(alias);
                        sInfo.addOperates("select");
                    } else throw new ParserException("tableSource[" + tb.getClass() + "] todo...");
                    List<SQLSelectItem> items = sq.getSelectList();
                    if (items != null) {
                        for (SQLSelectItem item : items) {
                            String alias = item.getAlias();
                            SQLExpr expr = item.getExpr();
                            if (expr instanceof SQLIdentifierExpr) {
                                String cn = ((SQLIdentifierExpr) expr).getName();
                                SQLInfo.ColInfo colInfo = sInfo.getCol(cn);
                                colInfo.setAlias(alias);
                                colInfo.addOperates("select");
                            } else if (expr instanceof SQLAllColumnExpr) {

                            } else throw new ParserException("SQLSelectItem expr[" +
                                    expr.getClass() + "] todo...");
                        }
                    }
                    SQLOrderBy order = sq.getOrderBy();
                    if (order != null) {
                        List<SQLSelectOrderByItem> selectOrderByItems = order.getItems();
                        for (SQLSelectOrderByItem selectOrderByItem : selectOrderByItems) {
                            SQLExpr expr = selectOrderByItem.getExpr();
                            if (expr instanceof SQLIdentifierExpr) {
                                String cn = ((SQLIdentifierExpr) expr).getName();
                                SQLInfo.ColInfo colInfo = sInfo.getCol(cn);
                                colInfo.addOperates("select");
                            } else throw new ParserException("SQLSelectItem expr[" +
                                    expr.getClass() + "] todo...");
                        }
                    }
                    SQLExpr where = sq.getWhere();
                    if (where != null) {
//                        String cn = parseSQLExpr(where, map, "SQLSelectQueryWhere");
//                        SQLInfo.ColInfo colInfo = sInfo.getCol(cn);
//                        colInfo.setCn(cn);
//                        colInfo.addOperates("select");
//                        sInfo.addCol(colInfo);
                    }
                    if (!map.containsKey(fn)) map.put(fn, sqlInfo);
                }
            } else if (query instanceof SQLUnionQuery) {

            } else throw new ParserException("select query[" + query.getClass() + "] todo...");
        }
    }


//    private void parseWhere(SQLExpr expr, Map<String, SQLInfo> sMap,String s, String flag) {
//       if (expr instanceof SQLInSubQueryExpr) {
//            SQLInSubQueryExpr sqlInSubQueryExpr = (SQLInSubQueryExpr) expr;
//            SQLSelect select = sqlInSubQueryExpr.getSubQuery();
//            parseSelectStatement(sMap, select);
////             break2Str(sqlInSubQueryExpr.getExpr(), sMap, flag);
//        } else if (expr instanceof SQLBinaryOpExpr) {
//            SQLBinaryOpExpr sqlBinaryOpExpr = (SQLBinaryOpExpr) expr;
//            return null;
//        } else throw new ParserException(flag + " SQLExpr[" + expr.getClass() + "] todo...");
//    }

//    private void parseWhere(SQLExpr expr,  tb, Map<String, SQLInfo> map) {
//        if (expr == null) return;
//        Class clazz = expr.getClass();
//        if (Parenthesis.class.equals(clazz)) {
//            Parenthesis parenthesis = (Parenthesis) expr;
//            SQLExpr pe = parenthesis.getExpression();
//            parseWhere(pe, tb, map);
//        } else if (AndExpression.class.equals(clazz)) {
//            AndExpression and = (AndExpression) expr;
//            parseWhere(and.getLeftExpression(), tb, map);
//            parseWhere(and.getRightExpression(), tb, map);
//        } else if (OrExpression.class.equals(clazz)) {
//            OrExpression or = (OrExpression) expr;
//            parseWhere(or.getLeftExpression(), tb, map);
//            parseWhere(or.getRightExpression(), tb, map);
//        } else {
//            SQLExpr left;
//            SQLExpr right;
//            if (.class.equals(clazz)) {
//                left = ((EqualsTo) expr).getLeftExpression();
//                right = ((EqualsTo) expr).getRightExpression();
//            } else if (GreaterThan.class.equals(clazz)) {
//                left = ((GreaterThan) expr).getLeftExpression();
//                right = ((GreaterThan) expr).getRightExpression();
//            } else if (GreaterThanEquals.class.equals(clazz)) {
//                left = ((GreaterThanEquals) expr).getLeftExpression();
//                right = ((GreaterThanEquals) expr).getRightExpression();
//            } else if (MinorThan.class.equals(clazz)) {
//                left = ((MinorThan) expr).getLeftExpression();
//                right = ((MinorThan) expr).getRightExpression();
//            } else if (MinorThanEquals.class.equals(clazz)) {
//                left = ((MinorThanEquals) expr).getLeftExpression();
//                right = ((MinorThanEquals) expr).getRightExpression();
//            } else if (LikeExpression.class.equals(clazz)) {
//                left = ((LikeExpression) expr).getLeftExpression();
//                right = ((LikeExpression) expr).getRightExpression();
//            } else if (Between.class.equals(clazz)) {
//                left = ((Between) expr).getLeftExpression();
//                right = null;
//            } else if (InExpression.class.equals(clazz)) {
//                left = ((InExpression) expr).getLeftExpression();
//                ItemsList rl = ((InExpression) expr).getRightItemsList();
//                if (rl instanceof SubSelect) right = (SubSelect) rl;
//                else throw new JSQLParserException("in condition[" + rl.getClass() + "] is not support");
//            } else throw new JSQLParserException("where condition[" + expr.getClass() + "] is not support");
//
//            if (left instanceof Column) copyMap(map, parseColumn(tb, "select", (Column) left));
//            else if (left instanceof Function) copyMap(map, parseFunction(left, tb));
//            else throw new JSQLParserException("where left expression[" + left.getClass() + "] is not support");
//
//            if (right instanceof SubSelect) copyMap(map, parseSelect((SubSelect) right));
//            else if (right instanceof Column) copyMap(map, parseColumn(tb, "select", (Column) right));
//        }
//    }
}
