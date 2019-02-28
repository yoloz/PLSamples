package parser;

import bean.LSException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

public class QuerySqlTest {

    @Before
    public void setUp() {

    }

    @SuppressWarnings("unchecked")
    @Test
    public void parseSelectColumn() throws JSQLParserException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String sql = "select col1,test.col2,col3 from test";
        Select select = (Select) new CCJSqlParserManager().parse(new StringReader(sql));
        PlainSelect ps = (PlainSelect) select.getSelectBody();
        List<SelectItem> selects = ps.getSelectItems();
        QuerySql querySql = new QuerySql(sql);
        Method method = querySql.getClass().getDeclaredMethod("parseSelectItem", List.class);
        method.setAccessible(true);
        List<String> list = (List<String>) method.invoke(querySql, selects);
        assertEquals(3, list.size());
        assertEquals("col2", list.get(1));
    }

    @Test
    public void parseSelectTable() throws JSQLParserException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String sql = "select col1,test.col2 from test";
//        sql = "select * from (select * from test)";
        Select select = (Select) new CCJSqlParserManager().parse(new StringReader(sql));
        PlainSelect ps = (PlainSelect) select.getSelectBody();
        FromItem fromItem = ps.getFromItem();
        QuerySql querySql = new QuerySql(sql);
        Method method = querySql.getClass().getDeclaredMethod("parseTableName", FromItem.class);
        method.setAccessible(true);
        String name = (String) method.invoke(querySql, fromItem);
        assertEquals("test", name);
    }

    /**
     * to_date('2007-06-12 10:00:00', 'yyyy-mm-dd hh24:mi:ss')暂未处理
     */
    @Test
    public void parseWhere() throws JSQLParserException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String sql = "select * from test where (col3='test' and col1 like 'a?b') or " +
                "(col2>11 or col4 between 1 and 4) and (col4<=5.3)";
//        sql = "select * from test where date between '2019-02-28T09:43:10.224000' and '2019-02-28T09:43:10.225000'";
        Select select = (Select) new CCJSqlParserManager().parse(new StringReader(sql));
        PlainSelect ps = (PlainSelect) select.getSelectBody();
        Expression where = ps.getWhere();
        StringBuilder builder = new StringBuilder();
        QuerySql querySql = new QuerySql(sql);
        Method method = querySql.getClass().getDeclaredMethod("parseWhere", StringBuilder.class, Expression.class);
        method.setAccessible(true);
        method.invoke(querySql, builder, where);
        System.out.println(builder.toString());
    }

    @Test
    public void parseLimit() throws JSQLParserException, LSException {
        String sql = "select * from test limit 10";
//        sql = "select * from test limit 10,5";
        Select select = (Select) new CCJSqlParserManager().parse(new StringReader(sql));
        PlainSelect ps = (PlainSelect) select.getSelectBody();
        Limit limit = ps.getLimit();
        if (limit != null) {
            Expression offset = limit.getOffset();
            if (offset != null) throw new LSException("暂不只支持limit offset,count");
            Expression rowCount = limit.getRowCount();
            if (!LongValue.class.equals(rowCount.getClass()))
                throw new LSException("limit right data type[" + rowCount.getClass() + "]not support");
            System.out.println(((LongValue) rowCount).getBigIntegerValue().longValueExact());
        }
    }


    @After
    public void tearDown() {
    }
}