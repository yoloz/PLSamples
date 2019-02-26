package util;

import bean.ImmutablePair;
import bean.LSException;
import bean.Ssdb;
import org.apache.log4j.Logger;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.Response;
import org.nutz.ssdb4j.spi.SSDB;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 非线程安全
 * 在取完这批次数据后,将对应类型的数据key保存,增量取数据
 */
public class SsdbUtil extends Thread {

    private final Logger logger = Logger.getLogger(SsdbUtil.class);

    private final int limit = 500;
    private Object point;

    private String ip;
    private int port;
    private String name;
    private Ssdb.Type type;
    public final int timeout;

    private String indexName;

    public final ArrayBlockingQueue<ImmutablePair<Object, String>> queue =
            new ArrayBlockingQueue<>(limit + 1);

    public SsdbUtil(String ip, int port, String name, Ssdb.Type type, String indexName) {
        this(ip, port, name, type, indexName, 60000);
    }

    private SsdbUtil(String ip, int port, String name, Ssdb.Type type, String indexName, int timeout) {
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.type = type;
        this.indexName = indexName;
        this.timeout = timeout;
    }


    /**
     * 创建单连接
     *
     * @return SSDB {@link org.nutz.ssdb4j.spi.SSDB}
     */
    private SSDB connect() {
        return SSDBs.simple(ip, port, timeout);
    }

    private void poll() throws LSException {
        logger.debug("polling ssdb." + name + " data to index");
        initPoint();
        try (SSDB ssdb = this.connect()) {
            int remaining;
            do {
                long start = System.currentTimeMillis();
                List<ImmutablePair<Object, String>> pairs = pollOnce(ssdb);
                if (!pairs.isEmpty()) for (ImmutablePair<Object, String> pair : pairs) {
                    queue.put(pair);
                }
                remaining = pairs.size();
                long end = System.currentTimeMillis();
                logger.debug("pollOnce[" + limit + "] cost time[" + (end - start) + "] mills");
            } while (remaining > 0);
            try {
                SqlliteUtil.update("update ssdb set point=? where name=?", name, point);
            } catch (SQLException e) {
                throw new LSException("更新ssdb.[" + name + "]的point信息失败", e);
            }
        } catch (LSException e) {
            throw e;
        } catch (Exception e) {
            throw new LSException("poll ssdb." + name + " error", e);
        }
    }

    private void initPoint() throws LSException {
        try {
            List<Map<String, Object>> points = SqlliteUtil.query(
                    "select point from ssdb where name=?", indexName);
            switch (type) {
                case LIST:
                    if (points.size() == 0) point = 0;
                    else point = Integer.parseInt((String) points.get(0).get("point"));
                    break;
                case HASH:
                    if (points.size() == 0) point = "";
                    else point = points.get(0).get("point");
                    break;
                default:
                    throw new LSException("ssdb type [" + type + "] is not support...");
            }
            SqlliteUtil.insert("INSERT INTO ssdb(name,point)VALUES (?,?)", name, point);
        } catch (Exception e) {
            throw new LSException("初始化ssdb." + name + "的point信息出错", e);
        }
    }

    private List<ImmutablePair<Object, String>> pollOnce(SSDB ssdb) throws LSException {
        switch (type) {
            case LIST:
                return listScan(ssdb, (int) point);
            case HASH:
                return hashScan(ssdb, (String) point);
            default:
                throw new LSException("ssdb type [" + type + "] is not support...");
        }
    }

    private List<ImmutablePair<Object, String>> listScan(SSDB ssdb, int offset) {
        Response response = ssdb.qrange(name, offset, limit);
        if (response.datas.size() == 0) return Collections.emptyList();
        else {
//           response.datas.parallelStream()
//                    .map(v -> new String(v, SSDBs.DEFAULT_CHARSET))
//                    .collect(Collectors.toList());
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
        Response response = ssdb.hscan(name, key_start, "", limit);
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

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            this.poll();
        } catch (LSException e) {
            logger.error("polling ssdb." + name + " data interrupted by error", e);
        }
    }
}
