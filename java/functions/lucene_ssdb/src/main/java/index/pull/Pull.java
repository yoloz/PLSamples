package index.pull;

import bean.ImmutableTriple;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class Pull implements Runnable, Closeable {

    Logger logger;
    String indexName;
    ArrayBlockingQueue<ImmutableTriple<Object, String, String>> queue;
    int blockSec;
    boolean stop = false;

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

    abstract void poll() throws Exception;

    @Override
    public abstract void close();
}
