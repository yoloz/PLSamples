package util;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Constants {

    private static final Logger logger = Logger.getLogger(Constants.class);

    static final Path appDir;
    public static final Path indexDir;
    public static final Path logDir;
    public static final Path varDir;
    public static final int httpPort;
    public static final int pageCache;
    public static final int searchCache;
    public static final int totalIndex;
    public static final double RAMBuffer;

    static {
        String root_dir = System.getProperty("LSDir");
        Properties properties = new Properties();
        try {
            try (InputStream inputStream = Files.newInputStream(Paths.get(root_dir,
                    "conf", "server.properties"))) {
                properties.load(inputStream);
            }
        } catch (Exception e) {
            logger.error("load server.properties error", e);
            System.exit(1);
        }
        appDir = Paths.get(root_dir);
        logDir = appDir.resolve("logs");
        varDir = appDir.resolve("var");
        String _indexDir = properties.getProperty("indexDir");
        if (_indexDir == null || _indexDir.isEmpty()) indexDir = varDir.resolve("index");
        else indexDir = Paths.get(_indexDir);
        httpPort = Integer.parseInt(properties.getProperty("httpPort"));
        pageCache = Integer.parseInt(properties.getProperty("pageCache", "10"));
        searchCache = Integer.parseInt(properties.getProperty("searchCache", "5"));
        totalIndex = Integer.valueOf(properties.getProperty("totalIndex", "6"));
        RAMBuffer = Double.parseDouble(properties.getProperty("indexBuffer", "128"));
    }
}
