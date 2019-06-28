package com.jdbc.sql.parser;

import com.jdbc.bean.SqlInfo;
import com.jdbc.util.JdbcConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SqlParseTest {


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testAlter() {
        String sql = "ALTER TABLE Persons ADD Birthday date";
        sql = "ALTER TABLE Persons ALTER COLUMN Birthday year";
        sql = "ALTER TABLE db.Persons DROP COLUMN Birthday";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(1, sqlInfoList.size());
        assertEquals("Persons", sqlInfoList.get(0).getName());
        assertTrue(sqlInfoList.get(0).getOperators().contains("alter"));
    }

    @Test
    public void testCreateIndex() {
        String sql = "CREATE INDEX PersonIndex ON Person (LastName DESC)";
        sql = "CREATE INDEX PersonIndex ON Person (LastName,FirstName)";
        sql = "CREATE UNIQUE INDEX PersonIndex ON db.Person (LastName,FirstName)";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals("Person", sqlInfoList.get(0).getName());
        assertTrue(sqlInfoList.get(0).getOperators().contains("createindex"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateTable() {
        String sql = "CREATE TABLE Persons (Id_P int,LastName varchar(255),FirstName varchar(255)," +
                "Address varchar(255),City varchar(255))";
        sql = "CREATE TABLE Persons AS SELECT id, address, city, state, zip FROM companies WHERE id1> 1000";
        sql = "CREATE TABLE Persons AS (SELECT id, address, city, state, zip FROM companies WHERE id1> 1000)";
        sql = "CREATE TABLE Persons AS (SELECT id, address FROM (select id, address from db.companies WHERE id1 > 1000))";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseCreate();
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals("Persons", sqlInfoList.get(0).getName());
        assertEquals(3, sqlInfoList.get(1).getCols().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateView() {
        String sql = "CREATE VIEW ProductsView AS SELECT ProductName,UnitPrice FROM Products " +
                "WHERE UnitPrice>(SELECT AVG(UnitPrice) FROM Products)";
        sql = "CREATE VIEW ProductsView AS SELECT ProductName,UnitPrice FROM Products " +
                "WHERE UnitPrice1>(SELECT AVG(UnitPrice) FROM Products1)";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseCreate();
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertTrue(sqlInfoList.get(0).getOperators().contains("createview"));
        assertEquals("ProductsView", sqlInfoList.get(0).getAlias());
        assertEquals(3, sqlInfoList.size());
    }

    @Test
    public void testDelete() {
        String sql = "DELETE FROM Person WHERE LastName = 'Wilson' ";
        sql = "delete from Person where S_date not in " +
                "(select e2.maxdt from" +
                "(select Order_Id as oid,Product_Id,Amt,MAX(S_date) as maxdt from Exam" +
                " group by Order_Id,Product_Id,Amt) as e2)";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseDeleteStatement();
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(2, sqlInfoList.size());
        assertEquals("Exam", sqlInfoList.get(1).getName());
        assertTrue(sqlInfoList.get(0).getOperators().contains("delete"));
        assertTrue(sqlInfoList.get(1).getOperators().contains("select"));
    }

    @Test
    public void testDrop() {
        String sql = "DROP TABLE Customer";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseDrop();
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(1, sqlInfoList.size());
        assertEquals("Customer", sqlInfoList.get(0).getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInsert() {
        String sql = "INSERT INTO Store_Information (Store_Name, Sales, Txn_Date) VALUES ('Los Angeles', 900, 'Jan-10-1999')";
        sql = "INSERT INTO Store_Information (Store_Name, Sales, Txn_Date) " +
                "SELECT store_name, Sales, Txn_Date FROM Sales_Information " +
                "WHERE Year(Txn_Date1) = 1998";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseInsert();
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(2, sqlInfoList.size());
        assertEquals("Sales_Information", sqlInfoList.get(1).getName());
        assertTrue(sqlInfoList.get(0).getOperators().contains("insert"));
        assertTrue(sqlInfoList.get(1).getOperators().contains("select"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSelect() {
        String sql = "select * from test where (col3='test')";
        sql = "select * from test where col1 like '北京' and date > '2019-02-28T09:43:10.224000'";
        sql = "select * from test where col1 like '北京' or date > '2019-02-28T09:43:10.224000'";
        sql = "select * from test where date between '2019-02-28T09:43:10.224000' and '2019-02-28T09:43:10.225000'";
        sql = "select * from test where date > '2019-02-28T09:43:10.224000'";
        sql = "select * from test where index between 10 and 20";
        sql = "select * from test where dd between 10.2 and 20.3";
        sql = "select * from test where col1=2";
        sql = "select * from test where col2='北京'";
        sql = "select * from test where col2 like '北京'";
        sql = "select * from test where (col3='test' and col1 like 'a?b')";
        sql = "select * from test where (col3='test' and col1 like 'a?b') or date>'2019-02-28T09:43:10.224000'";
        sql = "select * from test where (col3='test' and col1 like 'a?b') or" +
                " (col2>3 and date>'2019-02-28T09:43:10.224000')";
        sql = "select * from test where (col3='test' and col1 like 'a?b') or " +
                "(col2>3 or date>'2019-02-28T09:43:10.224000')";
        sql = "select * from test where (col3='test' and col1 like 'a?b') or " +
                "(col2>3 or date>'2019-02-28T09:43:10.224000') and col4='北京'";
        sql = "select * from test where (col3='test' and col1 like 'a?b') or " +
                "(col2>3 or date>'2019-02-28T09:43:10.224000') and (col5<=5.3)";
        sql = "SELECT id,name,time FROM table1 WHERE id2 IN (SELECT id3 FROM table2 WHERE name2 like 'z%')";
        sql = "SELECT T1.NAME,T2.CLASS FROM DB1.T1,DB2.T2 WHERE DB1.T1.ID=DB2.T2.ID";
        sql = "SELECT t1.NAME,t2.CLASS FROM DB1.T1 as t1,DB2.T2 as t2 WHERE t1.ID=t2.ID";
        sql = "SELECT T1.NAME,t2.CLASS FROM DB1.T1 as t1,DB2.T2 as t2 WHERE t1.ID=DB2.T2.ID";
        sql = "select * from lgservice where service_id=? and service_name=?";
//        sql="select lid, l.aid, h.hid, zjhm, xm, hname, roomId, r.rid, l.rent from leases l, applications," +
//                " applicants, houses h, rooms r where l.hid = h.hid and l.rid = r.rid and " +
//                "l.aid = applications.aid and applications.applicantZjhm = applicants.zjhm";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseSelect();
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(2, sqlInfoList.size());
    }


    @Test
    public void testTruncate() {
        String sql = "TRUNCATE TABLE Customer";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseTruncate();
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(1, sqlInfoList.size());
        assertTrue(sqlInfoList.get(0).getOperators().contains("truncate"));
    }

    @Test
    public void testUpdate() {
        String sql = "UPDATE Store_Information SET Sales = 500 WHERE Store_Name = 'Los Angeles' " +
                "AND Txn_Date = 'Jan-08-1999';";
        sql = "update Store_Information set shop_money=(select shop_money from build_info2 where build_info2.id=Store_Information.id)" +
                "where Store_Information.user = build_info2.user and Store_Information.user = 'test3' and shop_money =0";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
//        SQLStatement stmt = parser.parseUpdateStatement();
        parser.setDefaultDbName("test");
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(2, sqlInfoList.size());
        assertTrue(sqlInfoList.get(0).getOperators().contains("update"));
    }

    @Test
    public void testShow() {
        String sql = "show keys from bthost from fea_flow";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, JdbcConstants.MYSQL);
        Lexer lexer = parser.getLexer();
        assertEquals("SHOW", lexer.token().name);
        List<SqlInfo> sqlInfoList = parser.parseToSQLInfo();
        assertEquals(0, sqlInfoList.size());
    }
}