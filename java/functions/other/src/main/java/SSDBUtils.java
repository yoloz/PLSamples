import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.Response;
import org.nutz.ssdb4j.spi.SSDB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class SSDBUtils {

    private static final Logger logger = Logger.getLogger(SSDBUtils.class);

    private enum Type {
        kv, list, set, hash
    }

    private SSDB ssdb;
    private final int total = 2000000000;
    private static final int threads = 50;

    private final NewKey key = new NewKey();

    private final String value = "{\"facility_label\":\"kernel\",\"district\":\"彭州市\",\"icscompany_name\":\"北京三维力控科技有限公司\"}";

    private SSDBUtils() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(threads);
        config.setMaxIdle(threads);
        this.ssdb = SSDBs.pool("10.68.120.111", 6379, 10000, config);
    }

    /**
     * 在数据较多时清除很慢,直接手动删除磁盘文件然后重启
     */
    private void clear() {
        Response resp = ssdb.flushdb("");
        if (!resp.ok()) {
            logger.info("数据删除失败");
            close();
            System.exit(1);
        }
    }

    private class WriteDb extends Thread {
        private final Logger logger = getLogger();

        private Type type;
        private int _total;
        private CountDownLatch downLatch;

        private WriteDb(Type type, int total, CountDownLatch latch) {
            this.type = type;
            this._total = total;
            this.downLatch = latch;
        }

        @Override
        public void run() {
            logger.debug(getName() + " start...");
            switch (type) {
                case kv:
                    kv(getName());
                    break;
                case list:
                    list(getName());
                    break;
                case set:
                    set(getName());
                    break;
                case hash:
                    hash(getName());
                    break;
            }
            downLatch.countDown();
        }

        private void kv(String name) {
            long start = System.currentTimeMillis();
            long period = 10 * 60 * 1000;
            long counter = period;
            for (int i = 0; i < _total; i++) {
                String _key = key.create();
                if ((System.currentTimeMillis() - start) > counter) {
                    logger.info(name + "[key:]=>" + _key);
                    counter += period;
                }
                ssdb.set(_key, value);
            }
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info(name + "[kv 耗时:]=>" + use_time);
        }

        private void list(String name) {
            long start = System.currentTimeMillis();
            long counter = _total / 100;
            Object[] values = new Object[100];
            for (int i = 0; i < 100; i++) values[i] = value;
            for (int i = 0; i < counter; i++) {
                ssdb.qpush_front(name, values);
            }
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info(name + "[list 耗时:]=>" + use_time + " [list eps:]=>" + _total / use_time);
        }

        private void set(String name) {
            long start = System.currentTimeMillis();
            long period = 10 * 60 * 1000;
            long counter = period;
            for (int i = 0; i < _total; i++) {
                String _key = key.create();
                if ((System.currentTimeMillis() - start) > counter) {
                    logger.info(name + "[key:]=>" + _key);
                    counter += period;
                }
                ssdb.zset(name, _key, i);
            }
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info(name + "[set 耗时:]=>" + use_time + " [set eps:]=>" + _total / use_time);
        }

        private void hash(String name) {
            long start = System.currentTimeMillis();
            long period = 10 * 60 * 1000;
            long counter = period;
            for (int i = 0; i < _total; i++) {
                String _key = key.create();
                if ((System.currentTimeMillis() - start) > counter) {
                    logger.info(name + "[key:]=>" + _key);
                    counter += period;
                }
                ssdb.hset(name, _key, value);
            }
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info(name + "[hash 耗时:]=>" + use_time + " [hash eps:]=>" + _total / use_time);
        }

        private Logger getLogger() {
            String categoryName = "ssdbUtils-" + getName();
            String filePath = "/home/xxx/" + getName() + ".log";
            if (LogManager.exists(categoryName) != null) {
                return Logger.getLogger(categoryName);
            }
            try {
                String appendName = "A" + categoryName;
                PatternLayout layout = new PatternLayout();
                layout.setConversionPattern("\u0000%m\u0000\n");

                RollingFileAppender rollingFileAppender = new RollingFileAppender(layout, filePath, true);
                rollingFileAppender.setName(appendName);
                rollingFileAppender.setThreshold(Level.INFO);
                rollingFileAppender.setImmediateFlush(true);
                rollingFileAppender.setMaxBackupIndex(3);
                rollingFileAppender.setMaxFileSize("100MB");
                rollingFileAppender.setEncoding("UTF-8");

                Category category = Logger.getLogger(categoryName);
                category.setAdditivity(false);
                category.setLevel(Level.INFO);
                category.addAppender(rollingFileAppender);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Logger.getLogger(categoryName);
        }
    }

    private class QueryDb {
        private Type type;

        private QueryDb(Type type) {
            this.type = type;
        }

        private void query(Object... queryKey) {
            switch (type) {
                case kv:
                    kv(queryKey);
                    break;
                case list:
                    list(queryKey);
                    break;
                case set:
                    set(queryKey);
                    break;
                case hash:
                    hash(queryKey);
                    break;
            }
        }

        private void kv(Object... queryKey) {
            long start = System.currentTimeMillis();
            Response response;
            if (queryKey.length == 1) response = ssdb.get(queryKey);//get获取的response中[value]
            else response = ssdb.multi_get(queryKey); //multi_get获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[kv query 耗时:]=>" + use_time);
            logger.info("[kv query 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void list(Object... queryKey) {

        }

        private void set(Object... queryKey) {

        }

        private void hash(Object... queryKey) {

        }
    }

    private class ScanDb {
        private Type type;

        private ScanDb(Type type) {
            this.type = type;
        }

        private void scan(Object startK, Object endK, int limit) {
            switch (type) {
                case kv:
                    kv(startK, endK, limit);
                    break;
                case list:
                    list(startK, endK, limit);
                    break;
                case set:
                    set(startK, endK, limit);
                    break;
                case hash:
                    hash(startK, endK, limit);
                    break;
            }
        }

        private void kv(Object startK, Object endK, int limit) {
            long start = System.currentTimeMillis();
            Response response = ssdb.scan(start, endK, limit);//multi_get获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[kv scan 耗时:]=>" + use_time);
            logger.info("[kv scan 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void list(Object startK, Object endK, int limit) {

        }

        private void set(Object startK, Object endK, int limit) {

        }

        private void hash(Object startK, Object endK, int limit) {

        }
    }

    private void close() {
        try {
            if (ssdb != null) ssdb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PropertyConfigurator.configure("/home/xxx/log4j.properties");
        SSDBUtils ssdbUtils = new SSDBUtils();

        CountDownLatch downLatch = new CountDownLatch(threads);
        int count = ssdbUtils.total / threads;
        for (int i = 0; i < threads; i++) {
            ssdbUtils.new WriteDb(Type.kv, count, downLatch).start();
        }
        try {
            downLatch.await();
        } catch (InterruptedException e) {
            logger.error(e);
        }

//        ssdbUtils.new QueryDb(Type.kv).query("5c346d98876f4d");
//        ssdbUtils.new QueryDb(Type.kv).query("5c346d97875ee9", "5c346d98876f4d", "5c346fefa0b9b7", "5c346ff0a0d7af");
//        ssdbUtils.new ScanDb(Type.kv).scan("5c346d97875ee9", "5c3501404ba146", 200);

        ssdbUtils.close();
        logger.info("*******************finish*******************");
    }

    private class NewKey {
        private final AtomicInteger NEXT_COUNTER = new AtomicInteger(new SecureRandom().nextInt());

        private final char[] HEX_CHARS = new char[]{
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        private NewKey() {
        }

        private String create() {
            int timestamp = (int) (System.currentTimeMillis() / 1000);
            int counter = NEXT_COUNTER.getAndIncrement() & 0x00ffffff;

            char[] chars = new char[14];
            int i = 0;
            for (byte b : toByteArray(timestamp, counter)) {
                chars[i++] = HEX_CHARS[b >> 4 & 0xF];
                chars[i++] = HEX_CHARS[b & 0xF];
            }
            return new String(chars);
        }

        private byte[] toByteArray(int timestamp, int counter) {
            ByteBuffer buffer = ByteBuffer.allocate(7);
            buffer.put(int3(timestamp));
            buffer.put(int2(timestamp));
            buffer.put(int1(timestamp));
            buffer.put(int0(timestamp));
            buffer.put(int2(counter));
            buffer.put(int1(counter));
            buffer.put(int0(counter));
            return buffer.array();
        }

        private byte int3(final int x) {
            return (byte) (x >> 24);
        }

        private byte int2(final int x) {
            return (byte) (x >> 16);
        }

        private byte int1(final int x) {
            return (byte) (x >> 8);
        }

        private byte int0(final int x) {
            return (byte) (x);
        }

    }
}
