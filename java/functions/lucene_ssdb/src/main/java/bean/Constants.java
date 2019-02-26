package bean;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class Constants {

    private static final Logger logger = Logger.getLogger(Constants.class);

    public static final String appDir;
    public static final String indexDir;
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
            logger.error("解析配置[" + Paths.get(root_dir,
                    "conf", "server.properties") + "]错误,退出系统.", e);
            System.exit(1);
        }
        appDir = root_dir;
        indexDir = properties.getProperty("indexDir");
        httpPort = Integer.parseInt(properties.getProperty("httpPort"));
    }
}
