package com.auth;

import com.source.Connect;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NamedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerifySql {

    private Connection connect;

    private String db;
    private String user;
    private String sql;

    public VerifySql(Connect connect, String sql) {
        this.connect = connect.getConnection();
        this.db = connect.getDb();
        this.user = connect.getUser();
        this.sql = sql;
    }

    public boolean check() throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        Map<String, Map<String, String>> operations = parseOperation(stmt);
        for (String tb : operations.keySet()) {
            Authority authority = new Authority(db, tb, user);
            Map<String, String> opm = operations.get(tb);
            for (String key : opm.keySet()) {
                List<String> privs = authority.getPrivilege(key);
                if (privs == null) return false;
                if (!privs.contains(opm.get(key))) return false;
            }
        }
        return true;
    }

    private List<String> getAllCol(String tableName) throws JSQLParserException {
        try (PreparedStatement ps = connect.prepareStatement("select * from " + tableName + " where 1=0")) {
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
            throw new JSQLParserException(e.getMessage(), e);
        }
    }

    /**
     * <tableName,<tableName|columnName,operation>>
     */
    private Map<String, Map<String, String>> parseOperation(Statement stmt) throws JSQLParserException {
        Map<String, Map<String, String>> map = new HashMap<>();
        String operate = getClassName(stmt.getClass()).toLowerCase();
        if (stmt instanceof Alter) {
            Alter alter = (Alter) stmt;
            String tb = alter.getTable().getName();
            map.put(tb, Collections.singletonMap(tb, operate));
        } else if (stmt instanceof CreateIndex) {
            CreateIndex createIndex = (CreateIndex) stmt;
            String tb = createIndex.getTable().getName();
            map.put(tb, Collections.singletonMap(tb, operate));
        } else if (stmt instanceof CreateTable) {
            CreateTable createTable = (CreateTable) stmt;
            String tb = createTable.getTable().getName();
            Map<String, String> _m = new HashMap<>(1);
            _m.put(tb, operate);
            map.put(tb, _m);
            if (createTable.getSelect() != null) mergeMap(map, parseSelect(createTable.getSelect()));
        } else if (stmt instanceof CreateView) {
            CreateView createView = (CreateView) stmt;
            String tb = createView.getView().getName();
            Map<String, String> _m = new HashMap<>(1);
            _m.put(tb, operate);
            map.put(tb, _m);
            if (createView.getSelect() != null) mergeMap(map, parseSelect(createView.getSelect()));
        } else if (stmt instanceof Delete) {
            Delete delete = (Delete) stmt;
            String tb = delete.getTable().getName();
            Map<String, String> _m = new HashMap<>(1);
            _m.put(tb, operate);
            map.put(tb, _m);
            if (delete.getWhere() != null) parseWhere(delete.getWhere(), tb, map);
        } else if (stmt instanceof Drop) {
            Drop drop = (Drop) stmt;
            String tb = drop.getName().getName();
            map.put(tb, Collections.singletonMap(tb, operate));
        } else if (stmt instanceof Insert) {
            Insert insert = (Insert) stmt;
            String tb = insert.getTable().getName();
            Map<String, String> _m = new HashMap<>(1);
            _m.put(tb, operate);
            map.put(tb, _m);
            if (insert.getSelect() != null) mergeMap(map, parseSelect(insert.getSelect()));
        } else if (stmt instanceof Select) {
            Select select = (Select) stmt;
            if (select.getSelectBody() != null) mergeMap(map, parseSelect(select));
            if (select.getWithItemsList() != null) throw new JSQLParserException("select...with is not support");
        } else if (stmt instanceof Truncate) {
            Truncate truncate = (Truncate) stmt;
            String tb = truncate.getTable().getName();
            map.put(tb, Collections.singletonMap(tb, operate));
        } else if (stmt instanceof Update) {
            Update update = (Update) stmt;
            if (update.getTables().size() > 1) throw new JSQLParserException("update table size>1 is not support");
            String tb = update.getTables().get(0).getName();
            Map<String, String> _m = new HashMap<>();
            _m.put(tb, operate);
            map.put(tb, _m);
            List<Column> columns = update.getColumns();
            if (columns != null) for (Column column : columns) map.get(tb).put(column.getColumnName(), operate);
            if (update.getWhere() != null) parseWhere(update.getWhere(), tb, map);
            if (update.getSelect() != null) mergeMap(map, parseSelect(update.getSelect()));
            List<Expression> expressions = update.getExpressions();
            if (expressions != null) for (Expression exp : expressions) {
                if (exp instanceof SubSelect) mergeMap(map, parseSelect((SubSelect) exp));
            }
        } else {
            throw new JSQLParserException("operation [" + operate + "] is not support");
        }
        return map;
    }


    private Map<String, Map<String, String>> parseSelect(Select select) throws JSQLParserException {
        Map<String, Map<String, String>> map = new HashMap<>();
        SelectBody selectBody = select.getSelectBody();
        List<WithItem> withs = select.getWithItemsList();
        if (withs != null && withs.size() > 0) throw new JSQLParserException("select...with解析暂未支持");
        if (selectBody != null) parseSelectBody(selectBody, map);
        return map;
    }

    private Map<String, Map<String, String>> parseSelect(SubSelect select) throws JSQLParserException {
        Map<String, Map<String, String>> map = new HashMap<>();
        SelectBody selectBody = select.getSelectBody();
        List<WithItem> withs = select.getWithItemsList();
        if (withs != null && withs.size() > 0) throw new JSQLParserException("select...with解析暂未支持");
        if (selectBody != null) parseSelectBody(selectBody, map);
        return map;
    }

    private void parseSelectBody(SelectBody selectBody, Map<String, Map<String, String>> map)
            throws JSQLParserException {
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            FromItem fromItem = plainSelect.getFromItem();
            List<SelectItem> selectItems = plainSelect.getSelectItems();
            Expression where = plainSelect.getWhere();
            List<OrderByElement> orderByElements = plainSelect.getOrderByElements();
            String tableName = null;
            if (fromItem instanceof SubSelect) {
                mergeMap(map, parseSelect((SubSelect) fromItem));
            } else if (fromItem instanceof Table) {
                Table table = (Table) fromItem;
                tableName = table.getName();
                if (!map.containsKey(tableName)) {
                    Map<String, String> rm = new HashMap<>();
                    rm.put(tableName, "select");
                    map.put(tableName, rm);
                }
            } else throw new JSQLParserException("select...from[" + fromItem.getClass() + "]解析暂未支持");
            if (tableName != null && selectItems != null && selectItems.size() > 0) {
                for (SelectItem selectItem : selectItems) {
                    if (selectItem instanceof AllColumns || selectItem instanceof AllTableColumns) {
                        List<String> cols = getAllCol(tableName);
                        for (String col : cols) {
                            map.get(tableName).put(col, "select");
                        }
                    } else {
                        Expression expression = ((SelectExpressionItem) selectItem).getExpression();
                        if (expression instanceof Column) parseColumn((Column) expression, tableName, map);
                        else if (expression instanceof Function) parseFunction(expression, tableName, map);
                        else throw new JSQLParserException("select expression[" + expression.getClass() +
                                    "] is not support");
                    }
                }
            }
            if (tableName != null && where != null) parseWhere(where, tableName, map);
            if (tableName != null && orderByElements != null) {
                for (OrderByElement order : orderByElements) {
                    Expression expression = order.getExpression();
                    if (expression instanceof Column) {
                        map.get(tableName).put(((Column) expression).getColumnName(), "select");
                    } else
                        throw new JSQLParserException("order expression[" + expression.getClass() + "] is not support");
                }
            }
        } else throw new JSQLParserException("select body[" + selectBody.getClass() + "]解析暂未支持");

    }

    private void parseColumn(Column col, String tb, Map<String, Map<String, String>> map) {
        Table t = col.getTable();
        if (t != null && !t.getName().equals(tb)) {
            if (map.containsKey(t.getName()))
                map.get(t.getName()).put(col.getColumnName(), "select");
            else {
                Map<String, String> _m = new HashMap<>(1);
                _m.put(col.getColumnName(), "select");
                map.put(t.getName(), _m);
            }
        } else map.get(tb).put(col.getColumnName(), "select");
    }

    private void parseWhere(Expression expression, String tb, Map<String, Map<String, String>> map)
            throws JSQLParserException {
        Class clazz = expression.getClass();
        if (Parenthesis.class.equals(clazz)) {
            Parenthesis parenthesis = (Parenthesis) expression;
            Expression pe = parenthesis.getExpression();
            parseWhere(pe, tb, map);
        } else if (AndExpression.class.equals(clazz)) {
            AndExpression and = (AndExpression) expression;
            parseWhere(and.getLeftExpression(), tb, map);
            parseWhere(and.getRightExpression(), tb, map);
        } else if (OrExpression.class.equals(clazz)) {
            OrExpression or = (OrExpression) expression;
            parseWhere(or.getLeftExpression(), tb, map);
            parseWhere(or.getRightExpression(), tb, map);
        } else {
            Expression left;
            Expression right;
            if (EqualsTo.class.equals(clazz)) {
                left = ((EqualsTo) expression).getLeftExpression();
                right = ((EqualsTo) expression).getRightExpression();
            } else if (GreaterThan.class.equals(clazz)) {
                left = ((GreaterThan) expression).getLeftExpression();
                right = ((GreaterThan) expression).getRightExpression();
            } else if (GreaterThanEquals.class.equals(clazz)) {
                left = ((GreaterThanEquals) expression).getLeftExpression();
                right = ((GreaterThanEquals) expression).getRightExpression();
            } else if (MinorThan.class.equals(clazz)) {
                left = ((MinorThan) expression).getLeftExpression();
                right = ((MinorThan) expression).getRightExpression();
            } else if (MinorThanEquals.class.equals(clazz)) {
                left = ((MinorThanEquals) expression).getLeftExpression();
                right = ((MinorThanEquals) expression).getRightExpression();
            } else if (LikeExpression.class.equals(clazz)) {
                left = ((LikeExpression) expression).getLeftExpression();
                right = ((LikeExpression) expression).getRightExpression();
            } else if (Between.class.equals(clazz)) {
                left = ((Between) expression).getLeftExpression();
                right = null;
            } else if (InExpression.class.equals(clazz)) {
                left = ((InExpression) expression).getLeftExpression();
                ItemsList rl = ((InExpression) expression).getRightItemsList();
                if (rl instanceof SubSelect) right = (SubSelect) rl;
                else throw new JSQLParserException("in condition[" + rl.getClass() + "] is not support");
            } else throw new JSQLParserException("where condition[" + expression.getClass() + "] is not support");
            if (left instanceof Column) parseColumn((Column) left, tb, map);
            else if (left instanceof Function) parseFunction(left, tb, map);
            else throw new JSQLParserException("where left expression[" + left.getClass() + "] is not support");

            if (right instanceof SubSelect) mergeMap(map, parseSelect((SubSelect) right));
            else if (right instanceof Column) parseColumn((Column) right, tb, map);
        }
    }

    private void mergeMap(Map<String, Map<String, String>> src, Map<String, Map<String, String>> dst) {
        for (String dk : dst.keySet()) {
            if (src.containsKey(dk)) src.get(dk).putAll(dst.get(dk));
            else src.put(dk, dst.get(dk));
        }
    }

    private void parseFunction(Expression expression, String tb, Map<String, Map<String, String>> map)
            throws JSQLParserException {
        Function function = (Function) expression;
        NamedExpressionList nl = function.getNamedParameters();
        if (nl != null)
            throw new JSQLParserException("select function[" + nl.getClass() + "] is not support");
        ExpressionList el = function.getParameters();
        if (el != null) {
            for (Expression ele : el.getExpressions()) {
                if (ele instanceof Column) {
                    map.get(tb).put(((Column) ele).getColumnName(), "select");
                } else
                    throw new JSQLParserException("select function expression[" + ele.getClass() + "] is not support");
            }
        }
    }

    private String getClassName(Class clazz) {
        String fullName = clazz.getName();
        return fullName.substring(fullName.lastIndexOf(".") + 1);
    }

}
