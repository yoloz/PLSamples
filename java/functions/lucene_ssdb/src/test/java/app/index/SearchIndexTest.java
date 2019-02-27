package app.index;

import bean.ImmutablePair;
import bean.LSException;
import bean.Schema;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import util.SqlliteUtil;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SearchIndexTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * (index int,city string,company text, time date('uuuu-MM-dd'T'HH:mm:ss.SSSSSS')
     * <p>
     * {"index":2,"company":"北京三维力控科技有限公司","time":"2019-02-27T23:15:34.406000","city":"hangzhou"}
     */
    @Test
    public void search() throws SQLException, LSException {
        System.setProperty("LSDir",
                Paths.get(this.getClass().getResource("/schema_template.yaml").getPath())
                        .getParent().toString());
        List<Map<String, Object>> list = SqlliteUtil.query("select value from schema where name=?", "test");
        assertEquals(1, list.size());
        Yaml yaml = new Yaml();
        Schema schema = yaml.loadAs((String) list.get(0).get("value"), Schema.class);
        SearchIndex searchIndex = new SearchIndex(schema);
        String right = "city:hangzhou AND index:3";
        right = "city:hangzhou";
        right = "company:\"北京\" AND hangzhou";
        ImmutablePair<String, String> pair = ImmutablePair.of("city", right);
        List<Map<String, Object>> result = searchIndex.search(pair, 5);
        assertEquals(5, result.size());

    }
}