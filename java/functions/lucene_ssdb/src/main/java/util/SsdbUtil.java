package util;

import bean.LSException;
import bean.Ssdb;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.Response;
import org.nutz.ssdb4j.spi.SSDB;

import java.sql.SQLException;

public class SsdbUtil {

    private final int limit = 1000;
    private final String sql = "select point from ssdb where name=?";

    private String ip;
    private int port;
    private String name;
    private Ssdb.Type type;
    private int timeout;

    private String indexName;

    public SsdbUtil(String ip, int port, String name, Ssdb.Type type, String indexName) {
        this(ip, port, name, type, indexName, 60000);
    }

    public SsdbUtil(String ip, int port, String name, Ssdb.Type type, String indexName, int timeout) {
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

    public String poll() throws LSException {
        try {
            Object point = SqlliteUtil.query(sql, indexName).get(0).get("point");
            switch (type) {
                case LIST:
                    if (point == null) point = 0;
                    return listScan((int) point);
                case HASH:
                    return hashScan((String) point);
                default:
                    throw new LSException("ssdb type [" + type + "] is not support...");
            }
        } catch (SQLException e) {
            throw new LSException("查询ssdb." + name + "的point信息出错", e);
        }

    }

    public String listScan(int offset) {
        SSDB ssdb = this.connect();
        long start = System.currentTimeMillis();
        Response response = ssdb.qrange(name, offset, limit);//qrange获取的response中[v1,v2,v3...]
        long use_time = (System.currentTimeMillis() - start) / 1000;
//        logger.info("[" + type.toString() + " qrange 耗时:]=>" + use_time);
        return null;
    }

    public String hashScan(String key_end) throws LSException {
        throw new LSException("暂未实现...");
    }
}
