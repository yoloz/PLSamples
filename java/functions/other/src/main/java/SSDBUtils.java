import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.curator.shaded.com.google.common.io.Files;
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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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
        this.ssdb = SSDBs.pool("10.68.120.111", 6379, 60000, config);
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
            logger.info(getName() + " start...");
            try {
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
            } catch (Exception e) {
                logger.error(e);
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
            String filePath = "/xxx/resources/" + getName() + ".log";
            if (LogManager.exists(categoryName) != null) {
                return Logger.getLogger(categoryName);
            }
            try {
                String appendName = "A" + categoryName;
                PatternLayout layout = new PatternLayout();
                layout.setConversionPattern("%d{ISO8601} %p %t %c - %m%n");

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
            if (response.datas.size() == 0) logger.warn(Arrays.toString(queryKey) + " lost data");
            else logger.info("[kv query 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void list(Object... queryKey) {
            long start = System.currentTimeMillis();
            Response response = ssdb.qget(queryKey[0], (int) queryKey[1]); //qget获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[list query 耗时:]=>" + use_time);
            if (response.datas.size() == 0) logger.warn(queryKey[0] + " lost data");
            else logger.info("[list query 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void set(Object... queryKey) {
            long start = System.currentTimeMillis();
            String name = (String) queryKey[0];
            Object[] keys = new Object[queryKey.length - 1];
            System.arraycopy(queryKey, 1, keys, 0, queryKey.length - 1);
            Response response;
            if (queryKey.length == 2) response = ssdb.zget(name, keys[0]);//zget获取的response中[value]
            else response = ssdb.multi_zget(name, keys); //multi_zget获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[set query 耗时:]=>" + use_time);
            if (response.datas.size() == 0) logger.warn(name + "=>" + Arrays.toString(keys) + " lost data");
            else logger.info("[set query 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void hash(Object... queryKey) {
            long start = System.currentTimeMillis();
            Object name = queryKey[0];
            Object[] keys = new Object[queryKey.length - 1];
            System.arraycopy(queryKey, 1, keys, 0, queryKey.length - 1);
            Response response;
            if (queryKey.length == 2) response = ssdb.hget(name, keys[0]);//hget获取的response中[value]
            else response = ssdb.multi_hget(name, keys); //multi_hget获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[hash query 耗时:]=>" + use_time);
            if (response.datas.size() == 0) logger.warn(Arrays.toString(keys) + " lost data");
            else logger.info("[hash query 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }
    }

    private class ScanDb {
        private Type type;

        private ScanDb(Type type) {
            this.type = type;
        }

        private void scan(Object name, Object startK, Object endK, int limit) {
            switch (type) {
                case kv:
                    kv(startK, endK, limit);
                    break;
                case list:
                    list(name, startK, limit);
                    break;
                case set:
                    set(name, startK, limit);
                    break;
                case hash:
                    hash(name, startK, endK, limit);
                    break;
            }
        }

        private void kv(Object startK, Object endK, int limit) {
            long start = System.currentTimeMillis();
            Response response = ssdb.scan(startK, endK, limit);//scan获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[kv scan 耗时:]=>" + use_time);
            if (response.datas.size() == 0) logger.warn(startK + "~" + endK + " lost data");
            else logger.info("[kv scan 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void list(Object name, Object startK, int limit) {
            long start = System.currentTimeMillis();
            Response response = ssdb.qrange(name, (int) startK, limit);//qrange获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[list scan 耗时:]=>" + use_time);
            if (response.datas.size() == 0) logger.warn(startK + " lost data");
            else logger.info("[list scan 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void set(Object name, Object startK, int limit) {
            long start = System.currentTimeMillis();
            Response response = ssdb.zscan(name, startK, 26013, 3457891, limit);//zscan获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[set scan 耗时:]=>" + use_time);
            if (response.datas.size() == 0) logger.warn(startK + " lost data");
            else logger.info("[set scan 内容:]=>" + response.datas.size() + "=>" + response.asString());
        }

        private void hash(Object name, Object startK, Object endK, int limit) {
            long start = System.currentTimeMillis();
            Response response = ssdb.hscan(name, startK, endK, limit);//hscan获取的response中[k1,v1,k2,v2...]
            long use_time = (System.currentTimeMillis() - start) / 1000;
            logger.info("[hash scan 耗时:]=>" + use_time);
            if (response.datas.size() == 0) logger.warn(startK + "~" + endK + " lost data");
            else logger.info("[hash scan 内容:]=>" + response.datas.size() + "=>" + response.asString());
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
//        PropertyConfigurator.configure("/xxx/resources/log4j.properties");
//        SSDBUtils ssdbUtils = new SSDBUtils();

     /*   CountDownLatch downLatch = new CountDownLatch(threads);
        int count = ssdbUtils.total / threads;
        for (int i = 0; i < threads; i++) {
//            ssdbUtils.new WriteDb(Type.kv, count, downLatch).start();
//            ssdbUtils.new WriteDb(Type.list, count, downLatch).start();
//            ssdbUtils.new WriteDb(Type.set, count, downLatch).start();
            ssdbUtils.new WriteDb(Type.hash, count, downLatch).start();
        }
        try {
            downLatch.await();
        } catch (InterruptedException e) {
            logger.error(e);
        }*/
/*************************************kv*************************************/
       /* ssdbUtils.new QueryDb(Type.kv).query("5c346d98876f4d");
        ssdbUtils.new QueryDb(Type.kv).query("5c346d97875ee9", "5c346d98876f4d", "5c346fefa0b9b7", "5c346ff0a0d7af");
        ssdbUtils.new ScanDb(Type.kv).scan("5c346d97875ee9", "5c3501404ba146",null, 200);*/
/*************************************list*************************************/
     /*   for (int i = 1; i <= threads; i++) {
            String key = "Thread-" + i;
            Response resp = ssdbUtils.ssdb.qsize(key);
            System.out.println(key + "count=>" + resp.asString());
        }*/

     /*   QueryDb list_query = ssdbUtils.new QueryDb(Type.list);
        for (int i = 1; i <= threads; i++) {
            String key = "Thread-" + i;
            list_query.query(key, new Random().nextInt(20000000));
        }*/

      /*  ScanDb list_scan = ssdbUtils.new ScanDb(Type.list);
        for (int i = 1; i <= threads; i++) {
            String key = "Thread-" + i;
            list_scan.scan(key, new Random().nextInt(20000000), null, 100);
        }*/
/*************************************set*************************************/
      /*  int total_count = 0;
        for (int i = 1; i <= threads; i++) {
            String key = "Thread-" + i;
            Response resp = ssdbUtils.ssdb.zsize(key);
            int _count = resp.asInt();
            total_count += _count;
            System.out.println(key + "count=>" + _count);
        }
        System.out.println("total count=>" + total_count);*/

       /* QueryDb set_query = ssdbUtils.new QueryDb(Type.set);
        Random random = new Random();
        for (int i = 1; i <= threads; i++) {
            String name = "Thread-" + i;
            Object[] keys = ssdbUtils.randomKeys(name, 2, random);//zget times=1
            Object[] arr = new Object[keys.length + 1];
            arr[0] = name;
            System.arraycopy(keys, 0, arr, 1, keys.length);
            set_query.query(arr);
        }*/

       /* ScanDb set_scan = ssdbUtils.new ScanDb(Type.set);
        Random random = new Random();
        for (int i = 1; i <= threads; i++) {
            String name = "Thread-" + i;
            set_scan.scan(name, ssdbUtils.randomKeys(name, 1, random)[0], null, 100);
        }*/


/*************************************hash*************************************/
       /* int total_count = 0;
        for (int i = 1; i <= threads; i++) {
            String key = "Thread-" + i;
            Response resp = ssdbUtils.ssdb.hsize(key);
            int _count = resp.asInt();
            total_count += _count;
            System.out.println(key + "count=>" + _count);
        }
        System.out.println("total count=>" + total_count);*/

      /*  QueryDb hash_query = ssdbUtils.new QueryDb(Type.hash);
        Random random = new Random();
        for (int i = 1; i <= threads; i++) {
            String name = "Thread-" + i;
            Object[] keys = ssdbUtils.randomKeys(name, 3, random);//hget times=1
            Object[] arr = new Object[keys.length + 1];
            arr[0] = name;
            System.arraycopy(keys, 0, arr, 1, keys.length);
            hash_query.query(arr);
        }*/

       /* ScanDb hash_scan = ssdbUtils.new ScanDb(Type.hash);
        Random random = new Random();
        for (int i = 1; i <= threads; i++) {
            String name = "Thread-" + i;
            Object[] keys = ssdbUtils.randomKeys(name, 2, random);
            String startK = (String) keys[0];
            String endK = (String) keys[1];
            System.out.println(startK + "==" + endK);
            if (startK.compareTo(endK) > 0) {
                startK = (String) keys[1];
                endK = (String) keys[0];
            }
            hash_scan.scan(name, startK, endK, 100);
        }*/

/**************************************************************************/

//        ssdbUtils.sumLogTime();

//        ssdbUtils.close();
        logger.info("*******************finish*******************");
    }

    private Object[] randomKeys(String fileName, int times, Random random) {
        Object[] keys = new Object[times];
        Path path = Paths.get("/xxx/resources/hash", fileName + ".log");
        try {
            List<String> lines = Files.readLines(path.toFile(), StandardCharsets.UTF_8);
            int max = lines.size() - 1;
            for (int i = 0; i < times; i++) {
                int index = random.nextInt(max);
                if (index == 0) index++;
                String line = lines.get(index);
                System.out.println(fileName + "====" + max + "==" + index + "==" + line);
                int start = line.indexOf("=>");
                if (start > 0) {
                    int end = start + 2 + 14;
                    keys[i] = line.substring(start + 2, end).trim();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return keys;
    }

    private void sumLogTime() {
        int total = 0;
        int counter = 0;
        Path dir = Paths.get("/xxx/resources/hash");
        for (File file : Objects.requireNonNull(dir.toFile().listFiles())) {
            try {
                List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
                String line = lines.get(lines.size() - 1);
                int start = line.indexOf("=>");
                if (start > 0) {
                    int end = line.indexOf("[", start);
                    total += Integer.parseInt(line.substring(start + 2, end).trim());
                    counter += 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("total=>" + total + ",counter=>" + counter + ",avg=>" + (total / counter));
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
