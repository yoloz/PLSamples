package crud;

import bean.Field;
import bean.LSException;
import bean.Schema;
import bean.Ssdb;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CREATE TABLE test
 * (col1 int, col2 string, col3 date('yyyy-MM-dd HH:mm:ss.SSS'))
 * source=ssdb.test1 addr='127.0.0.1:8888' type=list
 * analyser='org.apache.lucene.analysis.standard.StandardAnalyzer'
 * <p>
 * analyser如果没定义则使用默认的分析器{@link org.apache.lucene.analysis.standard.StandardAnalyzer}
 */
public class CreateIndex {

    private Logger logger = Logger.getLogger(CreateIndex.class);

    private final String sql;
    private final Yaml yaml = new Yaml();

    public CreateIndex(String sql) {
        this.sql = sql;
    }

    public void create() throws LSException {
        CreateTable table;
        try {
            table = (CreateTable) new CCJSqlParserManager().
                    parse(new StringReader(sql));
        } catch (JSQLParserException e) {
            throw new LSException("构建语句解析错误", e);
        }
        Schema schema = new Schema();
        schema.setIndex(table.getTable().getName());
        Map<String, String> indexOptions = getIndexOptions(table.getTableOptionsStrings());
        this.checkIndexOptions(indexOptions);
        schema.setAnalyser(indexOptions.getOrDefault("analyser",
                "org.apache.lucene.analysis.standard.StandardAnalyzer"));
        String from = indexOptions.get("from");
        if("ssdb".equals(from)){
            Ssdb ssdb = new Ssdb();
            ssdb.setAddr(indexOptions.get("addr"));
            ssdb.setName(indexOptions.get("name"));
            ssdb.setType(indexOptions.get("type"));
            schema.setSsdb(ssdb);
        }
        List<Field> fields = new ArrayList<>(table.getColumnDefinitions().size());
        for (ColumnDefinition column : table.getColumnDefinitions()) {
            Field field = new Field();
            String type = column.getColDataType().getDataType().toLowerCase();
            if ("date".equals(type)) {
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

    private void checkIndexOptions(Map<String, String> options) throws LSException {
        if (!options.containsKey("addr")) throw new LSException("创建语句中未定义addr");
        if (!options.containsKey("from")) throw new LSException("创建语句中source配置有误");
        String from = options.get("from");
        if ("ssdb".equals(from)) {
            if (!options.containsKey("name")) throw new LSException("创建语句的source未定义ssdb的名称");
            if (!options.containsKey("type")) throw new LSException("创建语句中未定义ssdb的类型");
        } else throw new LSException("对来源[" + from + "]的数据暂不支持");
    }

    private Map<String, String> getIndexOptions(List<?> options) throws LSException {
        Map<String, String> params = new HashMap<>(5);
        for (int i = 0; i < options.size(); i += 3) {
            String key = String.valueOf(options.get(i)).toLowerCase();
            String value = String.valueOf(options.get(i + 2));
            if (value.charAt(0) == '\'') value = value.substring(1, value.length() - 1);
            if ("source".equals(key)) {
                if (!value.contains(".")) throw new LSException("创建语句source[" + value + "]格式[type.name]错误");
                int index = value.indexOf(".");
                params.put("from", value.substring(0, index).toLowerCase());
                params.put("name", value.substring(index + 1));
            } else params.put(key, value);
        }

        return Collections.unmodifiableMap(params);
    }

}
