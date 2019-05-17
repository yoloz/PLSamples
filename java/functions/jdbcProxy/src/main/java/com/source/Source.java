package com.source;

import com.JPServer;
import com.jdbc.bean.DataSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unchecked")
public class Source {

    public static final ConcurrentMap<String, DataSource> mapper = new ConcurrentHashMap<>();

    static {

        try (InputStream in = Files.newInputStream(Paths.get(JPServer.JPPath, "conf", "mapping.yaml"))) {
            List<Object> list = new Yaml().loadAs(in, ArrayList.class);
            list.forEach(obj -> {
                DataSource dataSource = new DataSource((Map) obj);
                mapper.put(dataSource.getKeyword(), dataSource);
            });
        } catch (IOException e) {
            System.err.println("init source mapping error," + e);
            System.exit(1);
        }
    }


}