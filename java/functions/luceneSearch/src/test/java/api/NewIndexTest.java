package api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.SSDB;
import util.Utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class NewIndexTest {

    @Before
    public void setUp() throws Exception {
    }

    /**
     * curl localhost:12580/newIndex -X POST -d "CREATE TABLE test(index int,city string,company text,
     * time date('uuuu-MM-dd'T'HH:mm:ss.SSSSSS')) source=ssdb.listTest addr='127.0.0.1:8888' type=list"
     * curl localhost:12580/addIndex -X POST -d "test"
     * 测试前需要目录${LSDir}下满足bin/java,conf/*,lib/
     * 启动HttpServerTest.startHttpServer监听http请求
     *
     * @throws IOException error
     */
    @Test
    public void createListData() throws IOException, InterruptedException {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS");
        try (SSDB ssdb = SSDBs.simple()) {
            Object[] values = new Object[100];
            for (int j = 0; j < 10; j++) {
                for (int i = 0; i < 100; i++) {
                    Map<String, Object> value = new HashMap<>(5);
                    LocalDateTime lt = LocalDateTime.now();
                    value.put("city", "hangzhou");
                    value.put("company", "北京三维力控科技有限公司");
                    value.put("time", lt.format(dateTimeFormatter));
                    value.put("index", j * 100 + i);
                    value.put("timestamp", Utils.toNanos(lt));
                    values[i] = toJson(value);
                    Thread.sleep(0, 999999);
                }
                ssdb.qpush_back("listTest", values);
            }
        }
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        else {
            StringBuilder builder = new StringBuilder("{");
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                builder.append("\"").append(entry.getKey()).append("\"").append(":");
                if (entry.getValue() instanceof Integer) builder.append(entry.getValue()).append(",");
                else builder.append("\"").append(entry.getValue()).append("\",");
            }
            return builder.substring(0, builder.length() - 1) + "}";
        }
    }

    @After
    public void tearDown() throws Exception {
    }
}