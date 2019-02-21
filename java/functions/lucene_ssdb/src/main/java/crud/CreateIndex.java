package crud;

import bean.Field;
import bean.From;
import bean.LSException;
import bean.Schema;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * CREATE TABLE test
 * (col1 int, col2 string, col3 date('yyyy-MM-dd HH:mm:ss.SSS'))
 * type=ssdb addr='127.0.0.1:8888'
 */
public class CreateIndex {

    private Logger logger = Logger.getLogger(CreateIndex.class);

    private String sql;
    private CreateTable table;
    private Yaml yaml = new Yaml();

    public CreateIndex(String sql) {
        this.sql = sql;
    }

    public void create() throws LSException {
        try {
            table = (CreateTable) new CCJSqlParserManager().
                    parse(new StringReader(sql));
        } catch (JSQLParserException e) {
            throw new LSException("构建语句解析错误", e);
        }
        Schema schema = new Schema();
        schema.setIndex(table.getTable().getName());
        schema.setAnalyser("");
        From from = new From();
        from.setType();
        from.setAddr();
        schema.setFrom(from);
        List<Field> fields = new ArrayList<>(table.getColumnDefinitions().size());
        for (ColumnDefinition column : table.getColumnDefinitions()) {
            Field field = new Field();
            String type = column.getColDataType().getDataType();
            if ("date".equalsIgnoreCase(type)) {
                String formatter = column.getColDataType().getArgumentsStringList().get(0);
                if (formatter.charAt(0) == '\'') formatter = formatter.substring(1, formatter.length() - 1);
                field.setFormatter(formatter);
            }
            field.setName(column.getColumnName());
            field.setType(type);
            fields.add(field);
        }
        schema.setFields(fields);
        yaml.dump("");
    }

}
