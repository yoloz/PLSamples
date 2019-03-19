package index.pull;

import bean.ImmutableTriple;
import bean.LSException;
import bean.Ssdb;
import org.apache.log4j.Logger;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.Response;
import org.nutz.ssdb4j.spi.SSDB;
import util.SqlliteUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 连接后不断开持续取数据,无数据即阻塞;
 * 定量(10000)更新point点,在异常断开后重启可以继续[异常断连会造成丢失数据]
 */
public class SsdbPull extends Pull {

    private Object point;
    private final Ssdb ssdb;

    public SsdbPull(Ssdb ssdb, String name,
                    ArrayBlockingQueue<ImmutableTriple<Object, String, String>> queue,
                    int blockSec, Logger logger) {
        super(name, queue, blockSec, logger);
        this.ssdb = ssdb;
    }


    @Override
    void setName() {
        if(ssdb.getName().contains("*")){

        }else pullName.add(ssdb.getName());
    }

    @Override
    void poll() throws Exception {
        logger.info("start pull[" + indexName + "]data from ssdb");
        initPoint();
        try (SSDB ssdb = this.connect()) {
            int counter = 0;
            while (!stop) {
                long start = System.currentTimeMillis();
                List<ImmutableTriple<Object, String, String>> triples = pollOnce(ssdb);
                if (!triples.isEmpty()) for (ImmutableTriple<Object, String, String> triple : triples) {
                    queue.put(triple);
                }
                counter += triples.size();
                long end = System.currentTimeMillis();
                logger.debug("pollOnce[" + triples.size() + "] cost time[" + (end - start) + "] mills");
                if (triples.size() == 0) Thread.sleep(blockSec * 500);
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

    /**
     * 创建单连接
     *
     * @return SSDB {@link SSDB}
     */
    private SSDB connect() {
        return SSDBs.simple(ssdb.getIp(), ssdb.getPort(), (blockSec - 1) * 1000);
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

    private List<ImmutableTriple<Object, String, String>> pollOnce(SSDB ssdb) {
        if (Ssdb.Type.LIST == this.ssdb.getType()) return listScan(ssdb, (int) point);
        else return hashScan(ssdb, (String) point);
    }

    private List<ImmutableTriple<Object, String, String>> listScan(SSDB ssdb, int offset) {
        Response response = ssdb.qrange(this.ssdb.getName(), offset, queue.size() / 2);
        if (response.datas.size() == 0) return Collections.emptyList();
        else {
            List<ImmutableTriple<Object, String, String>> list = new ArrayList<>(response.datas.size());
            for (int i = 0; i < response.datas.size(); i++) {
                ImmutableTriple<Object, String, String> pair = ImmutableTriple.of(offset + i,
                        this.ssdb.getName(), new String(response.datas.get(i), SSDBs.DEFAULT_CHARSET));
                list.add(pair);
            }
            point = offset + response.datas.size();
            return list;
        }
    }

    private List<ImmutableTriple<Object, String, String>> hashScan(SSDB ssdb, String key_start) {
        Response response = ssdb.hscan(this.ssdb.getName(), key_start, "", queue.size() / 2);
        if (response.datas.size() == 0) return Collections.emptyList();
        else {
            List<ImmutableTriple<Object, String, String>> list = new ArrayList<>(response.datas.size());
            for (int i = 0; i < response.datas.size(); i += 2) {
                String key = new String(response.datas.get(i), SSDBs.DEFAULT_CHARSET);
                if (i == response.datas.size() - 2) point = key;
                ImmutableTriple<Object, String, String> pair = ImmutableTriple.of(key,
                        this.ssdb.getName(), new String(response.datas.get(i + 1), SSDBs.DEFAULT_CHARSET));
                list.add(pair);
            }
            return list;
        }
    }

}
