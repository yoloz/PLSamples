package index;

import bean.ImmutablePair;
import bean.LSException;
import bean.Ssdb;
import org.apache.log4j.Logger;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.Response;
import org.nutz.ssdb4j.spi.SSDB;
import util.SqlliteUtil;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 非线程安全
 * <p>
 * 连接后不断开持续取数据,无数据即阻塞;
 * 定量(10000)更新point点,在异常断开后重启可以继续[异常断连会造成丢失数据]
 */
public class SsdbPull extends Thread implements Closeable {

    private final Logger logger;

    private final int batch = 500;
    private Object point;

    private final Ssdb ssdb;
    private final String indexName;
    private final ArrayBlockingQueue<ImmutablePair<Object, String>> queue;


    private final int waitMills = 5000; //need lower than timeout[10000]
    private boolean stop = false;

    public SsdbPull(Ssdb ssdb, String name,
                    ArrayBlockingQueue<ImmutablePair<Object, String>> queue,
                    Logger logger) {
        this.ssdb = ssdb;
        this.indexName = name;
        this.queue = queue;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            this.poll();
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            close();
        }
    }

    @Override
    public void close() {
        logger.info("close[" + indexName + "]ssdb_pull...");
        this.stop = true;
        try {
            Thread.sleep(waitMills + 10);
        } catch (InterruptedException ignore) {
        }
    }

    public boolean isRunning() {
        return !stop;
    }

    /**
     * 创建单连接
     *
     * @return SSDB {@link SSDB}
     */
    private SSDB connect() {
        return SSDBs.simple(ssdb.getIp(), ssdb.getPort(), 10000);
    }

    private void poll() throws Exception {
        logger.info("start pull[" + indexName + "]data from ssdb");
        initPoint();
        try (SSDB ssdb = this.connect()) {
            int counter = 0;
            while (!stop) {
                long start = System.currentTimeMillis();
                List<ImmutablePair<Object, String>> pairs = pollOnce(ssdb);
                if (!pairs.isEmpty()) for (ImmutablePair<Object, String> pair : pairs) {
                    queue.put(pair);
                }
                counter += pairs.size();
                long end = System.currentTimeMillis();
                logger.debug("pollOnce[" + pairs.size() + "] cost time[" + (end - start) + "] mills");
                if (pairs.size() == 0) Thread.sleep(waitMills);
                if (counter >= 10000) {
                    SqlliteUtil.update("update ssdb set point=? where name=?", point, indexName);
                    counter = 0;
                }
            }
            SqlliteUtil.update("update ssdb set point=? where name=?", point, indexName);
        } catch (SQLException e) {
            logger.warn(e.getMessage(), e);
        }
        logger.info("stop pull[" + indexName + "]data from ssdb");
    }

    private void initPoint() throws LSException {
        try {
            List<Map<String, Object>> points = SqlliteUtil.query(
                    "select point from ssdb where name=?", indexName);
            if (Ssdb.Type.LIST == ssdb.getType())
                point = Integer.parseInt(String.valueOf(points.get(0).get("point")));
            else point = points.get(0).get("point");
        } catch (SQLException e) {
            throw new LSException("初始化" + indexName + "的point信息出错", e);
        }
    }

    private List<ImmutablePair<Object, String>> pollOnce(SSDB ssdb) {
        if (Ssdb.Type.LIST == this.ssdb.getType()) return listScan(ssdb, (int) point);
        else return hashScan(ssdb, (String) point);
    }

    private List<ImmutablePair<Object, String>> listScan(SSDB ssdb, int offset) {
        Response response = ssdb.qrange(this.ssdb.getName(), offset, batch);
        if (response.datas.size() == 0) return Collections.emptyList();
        else {
            List<ImmutablePair<Object, String>> list = new ArrayList<>(response.datas.size());
            for (int i = 0; i < response.datas.size(); i++) {
                ImmutablePair<Object, String> pair = ImmutablePair.of(offset + i,
                        new String(response.datas.get(i), SSDBs.DEFAULT_CHARSET));
                list.add(pair);
            }
            point = offset + response.datas.size();
            return list;
        }
    }

    private List<ImmutablePair<Object, String>> hashScan(SSDB ssdb, String key_start) {
        Response response = ssdb.hscan(this.ssdb.getName(), key_start, "", batch);
        if (response.datas.size() == 0) return Collections.emptyList();
        else {
            List<ImmutablePair<Object, String>> list = new ArrayList<>(response.datas.size());
            for (int i = 0; i < response.datas.size(); i += 2) {
                String key = new String(response.datas.get(i), SSDBs.DEFAULT_CHARSET);
                if (i == response.datas.size() - 2) point = key;
                ImmutablePair<Object, String> pair = ImmutablePair.of(key,
                        new String(response.datas.get(i + 1), SSDBs.DEFAULT_CHARSET));
                list.add(pair);
            }
            return list;
        }
    }
}
