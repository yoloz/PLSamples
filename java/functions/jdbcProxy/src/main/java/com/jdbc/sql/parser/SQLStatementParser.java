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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.jdbc.bean.SqlInfo;
import com.jdbc.sql.ast.SQLExpr;
import com.jdbc.sql.ast.SQLOrderBy;
import com.jdbc.sql.ast.SQLStatement;
import com.jdbc.sql.ast.expr.SQLAggregateExpr;
import com.jdbc.sql.ast.expr.SQLAllColumnExpr;
import com.jdbc.sql.ast.expr.SQLBetweenExpr;
import com.jdbc.sql.ast.expr.SQLBinaryOpExpr;
import com.jdbc.sql.ast.expr.SQLCharExpr;
import com.jdbc.sql.ast.expr.SQLIdentifierExpr;
import com.jdbc.sql.ast.expr.SQLInSubQueryExpr;
import com.jdbc.sql.ast.expr.SQLIntegerExpr;
import com.jdbc.sql.ast.expr.SQLMethodInvokeExpr;
import com.jdbc.sql.ast.expr.SQLNumberExpr;
import com.jdbc.sql.ast.expr.SQLPropertyExpr;
import com.jdbc.sql.ast.expr.SQLQueryExpr;
import com.jdbc.sql.ast.expr.SQLVariantRefExpr;
import com.jdbc.sql.ast.statement.*;
import com.jdbc.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.jdbc.util.FnvHash;
import org.apache.log4j.Logger;

public class SQLStatementParser extends SQLStatementParsers {

    private final Logger logger = Logger.getLogger(SQLStatementParser.class);

    private String defaultDbName = "";
    private Connection conn;
    private List<SqlInfo> sqlInfoList;

    public SQLStatementParser(String sql) {
        super(sql);
    }

    public SQLStatementParser(String sql, String dbType) {
        super(sql, dbType);
    }

    public SQLStatementParser(SQLExprParser exprParser) {
        super(exprParser);
    }

    public SQLStatementParser(Lexer lexer, String dbType) {
        super(lexer, dbType);
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public void setDefaultDbName(String defaultDbName) {
        this.defaultDbName = defaultDbName;
    }

    public List<SqlInfo> parseToSQLInfo() {
        if ("select @@session.tx_read_only".equals(lexer.text)
                && lexer.token == Token.SELECT) {
            SQLSelect select = new SQLSelect();
            MySqlSelectQueryBlock queryBlock = new MySqlSelectQueryBlock();
            queryBlock.addSelectItem(new SQLPropertyExpr(new SQLVariantRefExpr("@@session"), "tx_read_only"));
            select.setQuery(queryBlock);

            SQLSelectStatement stmt = new SQLSelectStatement(select);

            parseSelect(stmt.getSelect());

            lexer.reset(29, '\u001A', Token.EOF);
            return sqlInfoList;
        }

        for (; ; ) {
            switch (lexer.token) {
                case EOF:
                case END:
                case UNTIL:
                case ELSE:
                case WHEN:
                    if (lexer.isKeepComments() && lexer.hasComment()) lexer.readAndResetComments();
                    return sqlInfoList;
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
                    logger.info("statement[" + stmt.getClass() + "] todo authentication");
                    continue;
                }
                case SELECT: {
                    SQLSelectStatement stmt = (SQLSelectStatement) parseSelect();
                    parseSelect(stmt.getSelect());
                    continue;
                }
                case UPDATE: {
                    SQLUpdateStatement stmt = parseUpdateStatement();
                    List<SqlInfo> list = new ArrayList<>(1);
                    parseTable(stmt.getTableSource(), "update", list);
                    if (!list.isEmpty()) {
                        List<SQLUpdateSetItem> updateSetItems = stmt.getItems();
                        for (SQLUpdateSetItem item : updateSetItems) {
                            parseColumn(item.getColumn(), null, list, "update");
                            parseColumn(item.getValue(), null, list, "update");
                        }
                        parseWhere(stmt.getWhere(), list);
                    }
                    continue;
                }
                case CREATE: {
                    parseCreateStatement(parseCreate());
                    continue;
                }
                case INSERT: {
                    SQLInsertStatement stmt = (SQLInsertStatement) parseInsert();
                    List<SQLExpr> columns = stmt.getColumns();
                    List<SqlInfo> list = new ArrayList<>(1);
                    parseTable(stmt.getTableSource(), "insert", list);
                    if (!list.isEmpty() && columns != null) for (SQLExpr column : columns) {
                        parseColumn(column, null, list, "insert");
                    }
                    parseSelect(stmt.getQuery());
                    continue;
                }
                case DELETE: {
                    SQLDeleteStatement stmt = parseDeleteStatement();
                    List<SqlInfo> list = new ArrayList<>(1);
                    parseTable(stmt.getTableSource(), "delete", list);
                    if (!list.isEmpty()) parseWhere(stmt.getWhere(), list);
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
                    String dbName = Objects.toString(stmt.getSchema(), defaultDbName);
                    SqlInfo sqlInfo = addTableInfo(dbName, stmt.getTableName());
                    sqlInfo.addOperator("alter");
                    continue;
                }
                case TRUNCATE: {
                    SQLTruncateStatement stmt = (SQLTruncateStatement) parseTruncate();
                    List<SQLExprTableSource> exprTableSources = stmt.getTableSources();
                    for (SQLExprTableSource tableSource : exprTableSources) {
                        parseTable(tableSource, "truncate", null);
                    }
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
                    parseDropStatement(stmt);
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
                    parseSelect(((SQLSelectStatement) stmt).getSelect());
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

    private void parseCreateStatement(SQLStatement stmt) {
        if (stmt instanceof SQLCreateTableStatement) {
            SQLCreateTableStatement createTableStatement = (SQLCreateTableStatement) stmt;
            parseTable(createTableStatement.getTableSource(), "createtable", null);
//            List<SQLTableElement> tableElements = createTableStatement.getTableElementList();
//            for (SQLTableElement element : tableElements) {
//                if (element instanceof SQLColumnDefinition) {
//                    SQLColumnDefinition columnDefinition = (SQLColumnDefinition) element;
//                    parseColumn(columnDefinition.getName(), null, tableInfo, "createtable");
//                } else throw new ParserException("SQLTableElement[" + stmt.getClass() + "] todo...");
//            }
            parseSelect(createTableStatement.getSelect());
        } else if (stmt instanceof SQLCreateIndexStatement) {
            SQLCreateIndexStatement createIndexStatement = (SQLCreateIndexStatement) stmt;
            String dbName = Objects.toString(createIndexStatement.getSchema(), defaultDbName);
            SqlInfo sqlInfo = addTableInfo(dbName, createIndexStatement.getTableName());
            sqlInfo.addOperator("createindex");
        } else if (stmt instanceof SQLCreateViewStatement) {
            SQLCreateViewStatement createViewStatement = (SQLCreateViewStatement) stmt;
            parseTable(createViewStatement.getTableSource(), "createview", null);
            parseSelect(createViewStatement.getSubQuery());
        } else if (stmt instanceof SQLCreateDatabaseStatement) {
            logger.info("statement[" + stmt.getClass() + "] todo");
        } else if (stmt instanceof SQLCreateUserStatement) {
            logger.info("statement[" + stmt.getClass() + "] todo");
        } else throw new ParserException("statement[" + stmt.getClass() + "] todo...");
    }

    private void parseDropStatement(SQLStatement stmt) {
        if (stmt instanceof SQLDropTableStatement) {
            SQLDropTableStatement dropTableStatement = (SQLDropTableStatement) stmt;
            List<SQLExprTableSource> exprTableSources = dropTableStatement.getTableSources();
            for (SQLExprTableSource tableSource : exprTableSources) {
                parseTable(tableSource, "drop", null);
            }
        } else if (stmt instanceof SQLDropIndexStatement) {
            logger.info("statement[" + stmt.getClass() + "] todo");
        } else if (stmt instanceof SQLDropViewStatement) {
            logger.info("statement[" + stmt.getClass() + "] todo");
        } else if (stmt instanceof SQLDropDatabaseStatement) {
            logger.info("statement[" + stmt.getClass() + "] todo");
        } else if (stmt instanceof SQLDropUserStatement) {
            logger.info("statement[" + stmt.getClass() + "] todo");
        } else throw new ParserException("statement[" + stmt.getClass() + "] todo...");
    }

    private void parseSelect(SQLSelect select) {
        if (select == null) return;
        SQLSelectQuery query = select.getQuery();
        if (query instanceof SQLSelectQueryBlock) {
            SQLSelectQueryBlock selectQueryBlock = (SQLSelectQueryBlock) query;
            List<SqlInfo> list = new ArrayList<>(1);
            parseTable(selectQueryBlock.getFrom(), "select", list);
            if (!list.isEmpty()) {
                List<SQLSelectItem> items = selectQueryBlock.getSelectList();
                if (items != null) {
                    for (SQLSelectItem item : items) {
                        parseColumn(item.getExpr(), item.getAlias(), list, "select");
                    }
                }
                SQLOrderBy order = selectQueryBlock.getOrderBy();
                if (order != null) {
                    List<SQLSelectOrderByItem> selectOrderByItems = order.getItems();
                    for (SQLSelectOrderByItem selectOrderByItem : selectOrderByItems) {
                        parseColumn(selectOrderByItem.getExpr(), null, list, "select");
                    }
                }
                parseWhere(selectQueryBlock.getWhere(), list);
            }
        } else if (query instanceof SQLUnionQuery) {
            logger.info("statement[" + query.getClass() + "] todo");
        } else throw new ParserException("select query[" + query.getClass() + "] todo...");
    }

    private void parseColumn(SQLExpr expr, String alias, List<SqlInfo> tables, String operator) {
        if (expr == null) return;
        if (expr instanceof SQLIdentifierExpr) {
            if (tables.size() > 1) throw new ParserException("column need table prefix");
            String cn = ((SQLIdentifierExpr) expr).getName();
            tables.get(0).addCol(cn, alias, operator);
        } else if (expr instanceof SQLPropertyExpr) {
            SQLPropertyExpr propertyExpr = (SQLPropertyExpr) expr;
            String cn = propertyExpr.getName();
            String owner = Objects.toString(propertyExpr.getOwnernName(), "");
            String dbName = defaultDbName;
            String tb;
            if (owner.contains(".")) {
                int i = owner.indexOf(".");
                dbName = owner.substring(0, i);
                tb = owner.substring(i + 1);
            } else tb = owner;
            boolean newTable = true;
            for (SqlInfo table : tables) {
                if (!dbName.isEmpty()) {
                    if (table.getDbName().equals(dbName) && (table.getName().equals(tb)
                            || table.getAlias().equals(tb))) {
                        newTable = false;
                        table.addCol(cn, operator);
                        break;
                    }
                } else {
                    if (table.getName().equals(tb) || table.getAlias().equals(tb)) {
                        newTable = false;
                        table.addCol(cn, operator);
                        break;
                    }
                }
            }
            if (newTable) {
                SqlInfo sqlInfo = addTableInfo(dbName, tb);
                sqlInfo.addCol(cn, operator);
            }
        } else if (expr instanceof SQLAllColumnExpr) {
            if (tables.size() > 1) throw new ParserException("column need table prefix");
            List<String> cols = getAllCol(tables.get(0));
            for (String col : cols) {
                tables.get(0).addCol(col, operator);
            }
        } else if (expr instanceof SQLAggregateExpr) {
            SQLAggregateExpr aggregateExpr = (SQLAggregateExpr) expr;
            List<SQLExpr> arguments = aggregateExpr.getArguments();
            for (SQLExpr argument : arguments) {
                parseColumn(argument, null, tables, operator);
            }
        } else if (expr instanceof SQLQueryExpr) {
            parseSelect(((SQLQueryExpr) expr).getSubQuery());
        } else if (expr instanceof SQLNumberExpr || expr instanceof SQLIntegerExpr
                || expr instanceof SQLCharExpr || expr instanceof SQLVariantRefExpr) {
            //ignore value
        } else throw new ParserException("Column expr[" + expr.getClass() + "] todo...");
    }

    private void parseTable(SQLTableSource tableSource, String operator, List<SqlInfo> list) {
        if (tableSource instanceof SQLExprTableSource) {
            String alias = tableSource.getAlias();
            SQLExprTableSource exprTableSource = (SQLExprTableSource) tableSource;
            String dbName = exprTableSource.getSchema();
            String tbName;
            SQLExpr expr = ((SQLExprTableSource) tableSource).getExpr();
            if (expr instanceof SQLIdentifierExpr) {
                tbName = ((SQLIdentifierExpr) expr).getName();
            } else if (expr instanceof SQLPropertyExpr) {
                SQLPropertyExpr propertyExpr = (SQLPropertyExpr) expr;
                dbName = propertyExpr.getOwnernName();
                tbName = propertyExpr.getName();
            } else throw new ParserException("SQLExprTableSource expr[" + expr.getClass() + "] todo...");
            SqlInfo sqlInfo = addTableInfo(Objects.toString(dbName, defaultDbName), tbName, alias);
            sqlInfo.addOperator(operator);
            if (list != null) list.add(sqlInfo);
        } else if (tableSource instanceof SQLSubqueryTableSource) {
            parseSelect(((SQLSubqueryTableSource) tableSource).getSelect());
        } else if (tableSource instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinTableSource = (SQLJoinTableSource) tableSource;
            parseTable(joinTableSource.getLeft(), operator, list);
            parseTable(joinTableSource.getRight(), operator, list);
        } else throw new ParserException("SQLTableSource[" + tableSource.getClass() + "] todo...");
    }

    private void parseWhere(SQLExpr expr, List<SqlInfo> tables) {
        if (expr == null) return;
        if (expr instanceof SQLBinaryOpExpr) {
            SQLBinaryOpExpr sqlBinaryOpExpr = (SQLBinaryOpExpr) expr;
            SQLExpr left = sqlBinaryOpExpr.getLeft();
            if (left != null) parseWhere(left, tables);
            SQLExpr right = sqlBinaryOpExpr.getRight();
            if (right != null) parseWhere(right, tables);
        } else if (expr instanceof SQLIdentifierExpr) {
            parseColumn(expr, null, tables, "select");
        } else if (expr instanceof SQLBetweenExpr) {
            SQLBetweenExpr sqlBetweenExpr = (SQLBetweenExpr) expr;
            SQLExpr testExpr = sqlBetweenExpr.getTestExpr();
            if (testExpr != null) parseWhere(testExpr, tables);
            SQLExpr beginExpr = sqlBetweenExpr.getBeginExpr();
            if (beginExpr != null) parseWhere(beginExpr, tables);
            SQLExpr endExpr = sqlBetweenExpr.getEndExpr();
            if (endExpr != null) parseWhere(endExpr, tables);
        } else if (expr instanceof SQLInSubQueryExpr) {
            SQLInSubQueryExpr sqlInSubQueryExpr = (SQLInSubQueryExpr) expr;
            SQLExpr subExpr = sqlInSubQueryExpr.getExpr();
            if (subExpr != null) parseWhere(subExpr, tables);
            SQLSelect select = sqlInSubQueryExpr.getSubQuery();
            if (select != null) parseSelect(select);
        } else if (expr instanceof SQLQueryExpr) {
            parseSelect(((SQLQueryExpr) expr).getSubQuery());
        } else if (expr instanceof SQLMethodInvokeExpr) {
            SQLMethodInvokeExpr methodInvokeExpr = (SQLMethodInvokeExpr) expr;
            List<SQLExpr> exprs = methodInvokeExpr.getParameters();
            if (exprs != null) for (SQLExpr sqlExpr : exprs) {
                parseColumn(sqlExpr, null, tables, "select");
            }
        } else if (expr instanceof SQLPropertyExpr) {
            parseColumn(expr, null, tables, "select");
        } else if (expr instanceof SQLNumberExpr || expr instanceof SQLIntegerExpr
                || expr instanceof SQLCharExpr || expr instanceof SQLVariantRefExpr) {
            //ignore value
        } else throw new ParserException("where SQLExpr[" + expr.getClass() + "] todo...");
    }

    private SqlInfo addTableInfo(String dbName, String name) {
        return addTableInfo(dbName, name, name);
    }

    private SqlInfo addTableInfo(String dbName, String name, String alias) {
        if (sqlInfoList == null) sqlInfoList = new ArrayList<>(1);
        for (SqlInfo sqlInfo : sqlInfoList) {
            if (sqlInfo.getDbName().equals(dbName)) {
                String _name = sqlInfo.getName();
                String _alias = sqlInfo.getAlias();
                if (_name.equals(name) || _alias.equals(name)) return sqlInfo;
            }
        }
        SqlInfo info = new SqlInfo(dbName, name, alias);
        sqlInfoList.add(info);
        return info;
    }

    private List<String> getAllCol(SqlInfo sqlInfo) {
        try (PreparedStatement ps = conn.prepareStatement("select * from " + sqlInfo.toString() + " where 1=0")) {
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            List<String> list = new ArrayList<>(cols);
            for (int i = 1; i <= cols; i++) {
                list.add(rsmd.getColumnName(i));
            }
            rs.close();
            return list;
        } catch (SQLException e) {
            throw new ParserException("get all column info error", e);
        }
    }
}
