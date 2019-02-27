package parser;

import com.sun.org.apache.xpath.internal.operations.Or;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

public class QuerySqlTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void parseSQL() throws JSQLParserException {
        String sql = "select col1,col2 from test where (col3=test and col1 like ab) or col1=1 ";
        try {
            Select select = (Select) new CCJSqlParserManager().parse(new StringReader(sql));
            PlainSelect ps = (PlainSelect) select.getSelectBody();
            FromItem fromItem = ps.getFromItem();
            Class fromClass = fromItem.getClass();
            if (fromClass.equals(Table.class)) {
                Table table = (Table) fromItem;
                assertEquals("test", table.getName());
            }
            List<Table> tables = ps.getIntoTables();
            assertNull(tables);
            List<SelectItem> selects = ps.getSelectItems();
            assertEquals(2, selects.size());
            SelectExpressionItem selectItem = (SelectExpressionItem) selects.get(1);
            Column column = (Column) selectItem.getExpression();
            assertEquals("col2", column.getColumnName());
            Expression having = ps.getHaving();
            assertNull(having);
            Expression where = ps.getWhere();
            Class whereClass = where.getClass();
            assertEquals(whereClass, OrExpression.class);
            OrExpression or = (OrExpression) where;
            Expression left = or.getLeftExpression();
            Class leftClass = left.getClass();
            assertEquals(leftClass, Parenthesis.class);
            Expression actualLeft = ((Parenthesis) left).getExpression();
            Class actualLeftClass = actualLeft.getClass();
            assertEquals(actualLeftClass, AndExpression.class);
            Expression right = or.getRightExpression();
            Class rightClass = right.getClass();
            assertEquals(rightClass, EqualsTo.class);
            EqualsTo rightEuqal = (EqualsTo) right;
            Expression rl = rightEuqal.getLeftExpression();
            assertEquals(Column.class, rl.getClass());
            Expression rr = rightEuqal.getRightExpression();
            assertEquals(LongValue.class, rr.getClass());
            Limit limit = ps.getLimit();
            assertNull(limit);
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
    }
}