package util;

import bean.ImmutablePair;
import bean.Ssdb;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.SSDB;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SsdbUtilTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * 测试前需要创建文件${LSDir}/conf/server.properties,否则Constants解析失败直接退出
     */
    @Test
    public void queryPoint() throws SQLException {
        System.setProperty("LSDir",
                Paths.get(this.getClass().getResource("/schema_template.yaml").getPath())
                        .getParent().toString());
        String sql = "select point from ssdb where name=?";
        List<Map<String, Object>> points = SqlliteUtil.query(sql, "test");
        assertEquals(0, points.size());
        SqlliteUtil.update("delete from ssdb where name=?", "list");
        SqlliteUtil.update("delete from ssdb where name=?", "hash");
        SqlliteUtil.insert("INSERT INTO ssdb(name,point)VALUES (?,?)", "list", "2000");
        SqlliteUtil.insert("INSERT INTO ssdb(name,point)VALUES (?,?)", "hash", "abcdeft_245");
        assertEquals(2000, Integer.parseInt((String) SqlliteUtil.query(sql, "list").get(0).get("point")));
        assertEquals("abcdeft_245", SqlliteUtil.query(sql, "hash").get(0).get("point"));
    }

    private void createData(Ssdb.Type type) throws IOException {
        try (SSDB ssdb = SSDBs.simple()) {
            if (Ssdb.Type.LIST == type) {
                Object[] values = new Object[100];
                for (int i = 0; i < 100; i++) values[i] = "listTest_" + i;
                ssdb.qpush_back("listTest", values);
            } else {
                for (int i = 0; i < 100; i++) {
                    ssdb.hset("hashTest", "key_" + i, "hashTest_" + i);
                }
            }
        }
    }

    @Test
    public void listScan() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, NoSuchFieldException {
        System.setProperty("LSDir",
                Paths.get(this.getClass().getResource("/schema_template.yaml").getPath())
                        .getParent().toString());
//        createData(Ssdb.Type.LIST);
        SsdbUtil ssdbUtil = new SsdbUtil("127.0.1", 8888, "listTest", Ssdb.Type.LIST, "list");
        Method connect = ssdbUtil.getClass().getDeclaredMethod("connect");
        connect.setAccessible(true);
        SSDB ssdb = (SSDB) connect.invoke(ssdbUtil);
        Method listScan = ssdbUtil.getClass().getDeclaredMethod("listScan",SSDB.class,int.class);
        listScan.setAccessible(true);
        Field point = ssdbUtil.getClass().getDeclaredField("point");
        point.setAccessible(true);
        List<ImmutablePair<Object, String>> result = (List<ImmutablePair<Object, String>>) listScan.invoke(ssdbUtil, ssdb, 0);
        assertEquals(ImmutablePair.of(0,"listTest_0"), result.get(0));
        assertEquals(100, point.get(ssdbUtil));
        ssdb.close();
    }

    @Test
    public void hashScan() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        System.setProperty("LSDir",
                Paths.get(this.getClass().getResource("/schema_template.yaml").getPath())
                        .getParent().toString());
        createData(Ssdb.Type.HASH);
        SsdbUtil ssdbUtil = new SsdbUtil("127.0.1", 8888, "hashTest", Ssdb.Type.LIST, "hash");
        Method connect = ssdbUtil.getClass().getDeclaredMethod("connect");
        connect.setAccessible(true);
        SSDB ssdb = (SSDB) connect.invoke(ssdbUtil);
        Method hashScan = ssdbUtil.getClass().getDeclaredMethod("hashScan",SSDB.class,String.class);
        hashScan.setAccessible(true);
        Field point = ssdbUtil.getClass().getDeclaredField("point");
        point.setAccessible(true);
        List<ImmutablePair<Object, String>> result = (List<ImmutablePair<Object, String>>) hashScan.invoke(ssdbUtil, ssdb, "");
        assertEquals(ImmutablePair.of("key_0","hashTest_0"), result.get(0));
        assertEquals("key_99", point.get(ssdbUtil));
        ssdb.close();
    }
}