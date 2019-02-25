package crud;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public class CreateIndexTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void create() throws JSQLParserException {
        String sql = "CREATE TABLE test" +
                "(col1 int, col2 string, col3 date('yyyy-MM-dd HH:mm:ss.SSS'))" +
                " analyser='org.apache.lucene.analysis.standard.StandardAnalyzer' source=ssdb.test1 addr='127.0.0.1:8888' type=list";
        try {
            CreateTable table = (CreateTable) new CCJSqlParserManager().parse(new StringReader(sql));
            assertEquals("test", table.getTable().getName());
            assertEquals("ssdb.test1", table.getTableOptionsStrings().get(5));
            assertEquals("'127.0.0.1:8888'", table.getTableOptionsStrings().get(8));
            assertEquals(3, table.getColumnDefinitions().size());
            assertEquals("int", table.getColumnDefinitions().get(0).getColDataType().getDataType());
            ColumnDefinition date = table.getColumnDefinitions().get(2);
            assertEquals("col3", date.getColumnName());
            assertEquals("'yyyy-MM-dd HH:mm:ss.SSS'", date.getColDataType().getArgumentsStringList().get(0));
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }
}