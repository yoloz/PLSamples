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
    public static final int httpPort;

    static {
        String root_dir = System.getProperty("LSDir");
        Properties properties = new Properties();
        try {
            try (InputStream inputStream = Files.newInputStream(Paths.get(root_dir,
                    "conf", "server.properties"))) {
                properties.load(inputStream);
            }
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            System.exit(1);
        }
        appDir = Paths.get(root_dir);
        logDir = appDir.resolve("logs");
        indexDir = Paths.get(properties.getProperty("indexDir"));
        httpPort = Integer.parseInt(properties.getProperty("httpPort"));
    }
}
