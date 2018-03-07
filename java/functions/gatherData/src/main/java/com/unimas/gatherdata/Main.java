package com.unimas.gatherdata;


//import LocalLog;

import com.unimas.gatherdata.gather.file.GatherFile;
import com.unimas.gatherdata.output.Output;
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
            Output output = Output.getOutput(outType, kafkaAddress, kafkaTopic);

            String _gatherFile = config.getProperty("gather.file.enable", "false");
            boolean gatherFile = _gatherFile.isEmpty() ? false : Boolean.valueOf(_gatherFile);
            if(gatherFile){
                GatherFile gf = new GatherFile(config,output);
                gf.gather();
            }

            String _gatherSys = config.getProperty("gather.system.enable","false");
            boolean gatherSys = _gatherSys.isEmpty()?false: Boolean.valueOf(_gatherSys);






//            levelLog(logger, config.getProperty("gather.logger.level"));

//            Gather gather = new Gather(threads, intervalMills, pathStr, pathMode, outType, kafkaAddress, kafkaTopic);
//            Runtime.getRuntime().addShutdownHook(new Thread(gather::close));
//            gather.gather();
            output.close();
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
