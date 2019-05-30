package com.jdbc.bean;

import com.JPServer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FilterRow {

    private static Map<String, String> filters = new HashMap<>(1);

    static String filter(String key) {
        if (filters.containsKey(key)) return filters.get(key);
        return null;
    }

    static {
        try (InputStream in = Files.newInputStream(Paths.get(JPServer.JPPath, "conf", "filterRow.yaml"))) {
            ArrayList list = new Yaml().loadAs(in, ArrayList.class);
            for (Object o : list) {
                Map<String, String> map = (Map<String, String>) o;
                String key = map.get("dbkey") + map.get("user")
                        + map.get("db") + map.get("tb") + map.get("cn");
                filters.put(key, map.get("val"));
            }
        } catch (IOException e) {
            System.err.println("loading dbSource error " + e);
            System.exit(1);
        }
    }
}
