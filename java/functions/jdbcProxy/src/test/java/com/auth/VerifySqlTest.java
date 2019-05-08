package com.auth;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class VerifySqlTest {

    private VerifySql verifySql;
    private Method parseSelect;
    private Method parseSubSelect;
    private Method parseSelectBody;
    private Method parseColumn;
    private Method parseWhere;
    private Method mergeMap;
    private Method getClassName;

    @Before
    public void setUp() throws Exception {
        verifySql = new VerifySql(null, null);
        parseSelect = verifySql.getClass().getDeclaredMethod("parseSelect", Select.class);
        parseSelect.setAccessible(true);
        parseSubSelect = verifySql.getClass().getDeclaredMethod("parseSelect", SubSelect.class);
        parseSubSelect.setAccessible(true);
        parseSelectBody = verifySql.getClass().getDeclaredMethod("parseSelectBody", SelectBody.class, Map.class);
        parseSelectBody.setAccessible(true);
        parseColumn = verifySql.getClass().getDeclaredMethod("parseColumn", Column.class, String.class, Map.class);
        parseColumn.setAccessible(true);
        parseWhere = verifySql.getClass().getDeclaredMethod("parseWhere", Expression.class, String.class, Map.class);
        parseWhere.setAccessible(true);
        mergeMap = verifySql.getClass().getDeclaredMethod("mergeMap", Map.class, Map.class);
        mergeMap.setAccessible(true);
        getClassName = verifySql.getClass().getDeclaredMethod("getClassName", Class.class);
        getClassName.setAccessible(true);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAlter() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "ALTER TABLE Persons ADD Birthday date";
        sql = "ALTER TABLE Persons ALTER COLUMN Birthday year";
        sql = "ALTER TABLE Persons DROP COLUMN Birthday";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("alter", operate);
        Alter alter = (Alter) stmt;
        assertEquals("Persons", alter.getTable().getName());
    }

    @Test
    public void testCreateIndex() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "CREATE INDEX PersonIndex ON Person (LastName DESC)";
        sql = "CREATE INDEX PersonIndex ON Person (LastName,FirstName)";
        sql = "CREATE UNIQUE INDEX PersonIndex ON Person (LastName,FirstName)";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("createindex", operate);
        CreateIndex createIndex = (CreateIndex) stmt;
        assertEquals("Person", createIndex.getTable().getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateTable() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "CREATE TABLE Persons (Id_P int,LastName varchar(255),FirstName varchar(255)," +
                "Address varchar(255),City varchar(255))";
        sql = "CREATE TABLE Persons AS (SELECT id, address, city, state, zip FROM companies WHERE id1> 1000)";
        sql = "CREATE TABLE Persons AS (SELECT id, address FROM (select state,zip from companies WHERE id1 > 1000))";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("createtable", operate);
        CreateTable createTable = (CreateTable) stmt;
        Select select = createTable.getSelect();
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) parseSelect.invoke(verifySql, select);
        assertEquals("Persons", createTable.getTable().getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateView() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "CREATE VIEW ProductsView AS SELECT ProductName,UnitPrice FROM Products " +
                "WHERE UnitPrice>(SELECT AVG(UnitPrice) FROM Products)";
        sql = "CREATE VIEW ProductsView AS SELECT ProductName,UnitPrice FROM Products " +
                "WHERE UnitPrice1>(SELECT AVG(UnitPrice) FROM Products1)";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("createview", operate);
        CreateView createView = (CreateView) stmt;
        Select select = createView.getSelect();
        Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) parseSelect.invoke(verifySql, select);
        assertEquals("ProductsView", createView.getView().getName());
    }

    @Test
    public void testDelete() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "DELETE FROM Person WHERE LastName = 'Wilson' ";
        sql = "delete from Person where S_date not in " +
                "(select e2.maxdt from" +
                "(select Order_Id,Product_Id,Amt,MAX(S_date) as maxdt from Exam" +
                " group by Order_Id,Product_Id,Amt) as e2)";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("delete", operate);
        Delete delete = (Delete) stmt;
        assertEquals("Person", delete.getTable().getName());
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> _m = new HashMap<>(1);
        _m.put(delete.getTable().getName(), operate);
        map.put(delete.getTable().getName(), _m);
        parseWhere.invoke(verifySql, delete.getWhere(), delete.getTable().getName(), map);
        assertEquals(2, map.size());
    }

    @Test
    public void testDrop() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "DROP TABLE Customer;";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("drop", operate);
        Drop drop = (Drop) stmt;
        assertEquals("Customer", drop.getName().getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInsert() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "INSERT INTO Store_Information (Store_Name, Sales, Txn_Date) VALUES ('Los Angeles', 900, 'Jan-10-1999')";
        sql = "INSERT INTO Store_Information (Store_Name, Sales, Txn_Date) " +
                "SELECT store_name, Sales, Txn_Date FROM Sales_Information " +
                "WHERE Year(Txn_Date1) = 1998";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("insert", operate);
        Insert insert = (Insert) stmt;
        assertEquals("Store_Information", insert.getTable().getName());
        if (insert.getSelect() != null) {
            Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) parseSelect.invoke(verifySql, insert.getSelect());
            assertEquals(1, map.size());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSelect() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "SELECT id,name,time FROM table1 WHERE id2 IN (SELECT id3 FROM table2 WHERE name2 like 'z%')";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("select", operate);
        Select select = (Select) stmt;
        Map<String, Map<String, String>> map = null;
        if (select.getSelectBody() != null)
            map = (Map<String, Map<String, String>>) parseSelect.invoke(verifySql, select);
        assertNotNull(map);
    }

    @Test
    public void testTruncate() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "TRUNCATE TABLE Customer";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("truncate", operate);
        Truncate truncate = (Truncate) stmt;
        assertEquals("Customer", truncate.getTable().getName());
    }

    @Test
    public void testUpdate() throws JSQLParserException, InvocationTargetException, IllegalAccessException {
        String sql = "UPDATE Store_Information SET Sales = 500 WHERE Store_Name = 'Los Angeles' " +
                "AND Txn_Date = 'Jan-08-1999';";
        sql = "update Store_Information set shop_money=(select shop_money from build_info2 where build_info2.id=Store_Information.id)" +
                "where Store_Information.user = build_info2.user and Store_Information.user = 'test3' and shop_money =0";
        Statement stmt = CCJSqlParserUtil.parse(new StringReader(sql));
        String operate = ((String) getClassName.invoke(verifySql, stmt.getClass())).toLowerCase();
        assertEquals("update", operate);
        Update update = (Update) stmt;
        String tb = update.getTables().get(0).getName();
        Map<String, Map<String, String>> map = new HashMap<>();
        assertEquals("Store_Information", tb);
        Map<String, String> _m = new HashMap<>();
        _m.put(tb, operate);
        map.put(tb, _m);
        List<Column> columns = update.getColumns();
        if (columns != null) for (Column column : columns) map.get(tb).put(column.getColumnName(), operate);
        if (update.getWhere() != null) parseWhere.invoke(verifySql, update.getWhere(), tb, map);
        if (update.getSelect() != null)
            mergeMap.invoke(verifySql, map, parseSelect.invoke(verifySql, update.getSelect()));
        List<Expression> expressions = update.getExpressions();
        if (expressions != null) for (Expression exp : expressions) {
            if (exp instanceof SubSelect)
                mergeMap.invoke(verifySql, map, parseSubSelect.invoke(verifySql, (SubSelect) exp));
        }
        assertEquals(1, update.getColumns().size());
    }

}