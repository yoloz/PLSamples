package parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public class QuerySqlTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void parseSQL() throws JSQLParserException {
        String sql = "select col1,col2 from test where col3=test and col1 like ab";
        sql = "select * from test where col3=test and col1 like ab";
        try {
            Select select = (Select) new CCJSqlParserManager().parse(new StringReader(sql));
            select.
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
    }
}