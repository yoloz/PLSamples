package app;

import app.index.WriteIndex;
import bean.LSException;
import bean.Schema;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import util.SqlliteUtil;
import util.Utils;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AppServer {

    private static final Logger logger = Logger.getLogger(AppServer.class);

    /**
     * app.AppServer.main(indexName)
     *
     * @param args {@link String[]}
     */
    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("启动失败,参数[" + Arrays.toString(args) + "]错误");
            System.exit(1);
        }
        logger.info(args[0] + " starting...");
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            List<Map<String, Object>> list = SqlliteUtil
                    .query("select value from schema where name=?", args[0]);
            if (list.isEmpty()) throw new LSException("index[" + args[0] + "] is not exit");
            WriteIndex writeIndex = new WriteIndex(
                    new Yaml().loadAs((String) list.get(0).get("value"), Schema.class),
                    countDownLatch);
            Runtime.getRuntime().addShutdownHook(new Thread(writeIndex::close));
            writeIndex.start();
            Utils.updateAppStatus(ManagementFactory.getRuntimeMXBean().getName().split("@")[0],
                    args[0]);
            countDownLatch.await();
            Utils.updateAppStatus("0", args[0]);
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            System.err.println(e.getMessage());
            try {
                Utils.updateAppStatus("0", args[0]);
            } catch (SQLException e1) {
                logger.error(e1);
            }
            System.exit(1);
        }
//        logger.info(args[0] + " finished...");
    }

}
