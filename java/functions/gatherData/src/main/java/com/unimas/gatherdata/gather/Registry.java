package com.unimas.gatherdata.gather;

import com.unimas.gatherdata.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import LocalLog;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
//import java.util.logging.Logger;

public class Registry {

    //    private static final Logger logger = LocalLog.getLogger();
    private static final Logger logger = LoggerFactory.getLogger(Registry.class);
    private static final Path path = Paths.get(Main.data_dir.toString(), "registry");

    public static Map<String, String> get() {
        logger.debug(Thread.currentThread().getName() + "-registry: get-" + System.currentTimeMillis());
        File file = path.toFile();
        if (file.exists()) {
            Properties properties = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(file), Charset.forName("UTF-8"))) {
                properties.load(reader);
                return toMap(properties);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return Collections.emptyMap();
    }

    public static void write(Map<String, String> content) {
        logger.debug(Thread.currentThread().getName() + "-registry: write-" + System.currentTimeMillis());
        Properties properties = mapTo(content);
        if (!properties.isEmpty()) {
            File file = path.toFile();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), Charset.forName("UTF-8")))) {
                properties.store(writer, "");
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static Properties mapTo(Map<String, String> map) {
        Properties properties = new Properties();
        if (map != null) map.forEach(properties::setProperty);
        return properties;
    }

    private static Map<String, String> toMap(Properties properties) {
        Map<String, String> map = new HashMap<>();
        if (properties != null) properties.forEach((k, v) -> map.put((String) k, (String) v));
        return map;
    }
}