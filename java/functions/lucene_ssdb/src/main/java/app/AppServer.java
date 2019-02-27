package app;

import app.index.WriteIndex;
import bean.LSException;
import bean.Schema;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import util.SqlliteUtil;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AppServer {

    private static final Logger logger = Logger.getLogger(AppServer.class);

    /**
     * app.AppServer.main(indexName,create_append|delete|query)
     *
     * @param args {@link String[]}
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("启动失败,参数[" + Arrays.toString(args) + "]错误");
            System.exit(1);
        }
        logger.info(args[0] + "=>" + args[1] + " starting...");
        try {
            if ("create_append".equals(args[1])) {
                Yaml yaml = new Yaml();
                CountDownLatch countDownLatch = new CountDownLatch(1);
                List<Map<String, Object>> list = SqlliteUtil
                        .query("select value from schema where name=?", args[0]);
                if (list.isEmpty()) throw new LSException("index[" + args[0] + "] is not exit");
                WriteIndex writeIndex = new WriteIndex(
                        yaml.loadAs((String) list.get(0).get("value"), Schema.class),
                        countDownLatch);
                writeIndex.start();
                countDownLatch.await();
            } else if ("delete".equals(args[1])) {
            } else if ("query".equals(args[1])) {
            } else System.err.println("启动失败,命令[" + args[1] + "]未定义");
        } catch (LSException | SQLException | InterruptedException e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            System.err.println(e.getMessage());
            System.exit(1);
        }
        logger.info(args[0] + "=>" + args[1] + " finished...");
    }
}
