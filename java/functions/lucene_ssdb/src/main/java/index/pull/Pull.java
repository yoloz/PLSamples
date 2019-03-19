package index.pull;

import bean.ImmutableTriple;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class Pull implements Runnable, Closeable {

    Logger logger;
    String indexName;
    ArrayBlockingQueue<ImmutableTriple<Object, String, String>> queue;
    int blockSec;
    boolean stop = false;
    TreeSet<String> pullName = new TreeSet<>();

    Pull(String name,
         ArrayBlockingQueue<ImmutableTriple<Object, String, String>> queue,
         int blockSec, Logger logger) {
        this.indexName = name;
        this.queue = queue;
        this.blockSec = blockSec;
        this.logger = logger;
    }

    public boolean isRunning() {
        return !stop;
    }

    @Override
    public void run() {
        try {
            this.poll();
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            this.close();
        }
    }

    @Override
    public void close() {
        logger.info("close[" + indexName + "]ssdb_pull...");
        this.stop = true;
        try {
            Thread.sleep(blockSec * 500 + 10);
        } catch (InterruptedException ignore) {
        }
    }

    abstract void poll() throws Exception;

    abstract void setName();
}
