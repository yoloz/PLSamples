package com.unimas.gatherdata;


//import LocalLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

//import java.util.logging.Level;
//import java.util.logging.Logger;

public class Main {

    //        private static final Logger logger = LocalLog.getLogger();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String app_dir = System.getProperty("ga.base.dir");
    public static final Path log_dir = Paths.get(app_dir, "logs");
    public static final Path data_dir = Paths.get(app_dir, "data");

    public static void main(String[] args) {
        try {
            Properties config = new Properties();
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(
                    Paths.get(app_dir, "config", "gather.properties").toFile()),
                    Charset.forName("UTF-8"))) {
                config.load(reader);
            }
            int threads;
            try {
                threads = Integer.parseInt(config.getProperty("gather.num.threads"));
            } catch (NumberFormatException e) {
                logger.warn("gather.num.threads: " + e.getMessage());
                threads = Runtime.getRuntime().availableProcessors();
            }
            int intervalMills;
            try {
                intervalMills = Integer.parseInt(config.getProperty("gather.interval.sec"));
            } catch (NumberFormatException e) {
                logger.warn("gather.interval.sec: " + e.getMessage());
                intervalMills = 5;
            }
            String pathStr = config.getProperty("gather.file.paths");
            if (pathStr == null || pathStr.isEmpty())
                throw new Exception("gather.file.paths undefined or empty...");
            String pathMode = config.getProperty("gather.file.path.mode", "lazy");
            pathMode = pathMode.isEmpty() ? "lazy" : pathMode;
            if (!"lazy,active".contains(pathMode)) {
                logger.error("gather.file.path.mode: " + pathMode + " unsupported!");
                pathMode = "lazy";
            }
            String outType = config.getProperty("gather.output.type", "console");
            outType = outType.isEmpty() ? "console" : outType;

            String kafkaAddress = null, kafkaTopic = null;
            if ("kafka".equals(outType)) {
                kafkaAddress = config.getProperty("gather.kafka.address");
                kafkaTopic = config.getProperty("gather.kafka.topic");
                if (kafkaAddress == null || kafkaAddress.isEmpty() ||
                        kafkaTopic == null || kafkaTopic.isEmpty())
                    throw new Exception("gather.kafka.* undefined or empty...");
            }
//目录创建脚本中处理
//            File dataDir = data_dir.toFile(), logDir = log_dir.toFile();
//            if (!dataDir.exists()) dataDir.mkdirs();
//            if (!logDir.exists()) dataDir.mkdirs();

//            levelLog(logger, config.getProperty("gather.logger.level"));

            Gather gather = new Gather(threads, intervalMills, pathStr, pathMode, outType, kafkaAddress, kafkaTopic);
            Runtime.getRuntime().addShutdownHook(new Thread(gather::close));
            gather.gather();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
    }

/*    private static void levelLog(Logger logger, String level) {
        if (level == null || level.isEmpty()) return;
        switch (level) {
            case "SEVERE":
                logger.setLevel(Level.SEVERE);
                break;
            case "WARNING":
                logger.setLevel(Level.WARNING);
                break;
            case "INFO":
                logger.setLevel(Level.INFO);
                break;
            case "CONFIG":
                logger.setLevel(Level.CONFIG);
                break;
            case "FINE":
                logger.setLevel(Level.FINE);
                break;
            case "FINER":
                logger.setLevel(Level.FINER);
                break;
            case "FINEST":
                logger.setLevel(Level.FINEST);
                break;
            case "ALL":
                logger.setLevel(Level.ALL);
                break;
            default:
                logger.setLevel(Level.WARNING);
        }
    }*/
}
