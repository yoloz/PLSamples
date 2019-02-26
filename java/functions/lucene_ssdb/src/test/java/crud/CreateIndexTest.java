package crud;

import bean.LSException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.SqlliteUtil;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CreateIndexTest {


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void parseSQL() throws JSQLParserException {
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

    /**
     * 测试前需要创建文件${LSDir}/conf/server.properties,否则Constants解析失败直接退出
     */
    @Test
    public void createSchema() throws SQLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        System.setProperty("LSDir",
                Paths.get(this.getClass().getResource("/schema_template.yaml").getPath())
                        .getParent().toString());
        String sql = "CREATE TABLE test" +
                "(col1 int, col2 string, col3 date('yyyy-MM-dd HH:mm:ss.SSS'))" +
                " analyser='org.apache.lucene.analysis.standard.StandardAnalyzer' source=ssdb.test1 addr='127.0.0.1:8888' type=list";
        CreateIndex createIndex = new CreateIndex(sql);
        Method createSchema = createIndex.getClass().getDeclaredMethod("createSchema");
        createSchema.setAccessible(true);
        createSchema.invoke(createIndex);
        String checkSql = "select name from schema";
        List<Map<String, Object>> result = SqlliteUtil.query(checkSql);
        assertEquals("test", result.get(0).get("name"));
    }
}