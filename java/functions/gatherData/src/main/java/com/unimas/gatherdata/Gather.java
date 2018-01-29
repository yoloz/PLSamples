package com.unimas.gatherdata;

import com.unimas.gatherdata.gather.FileWatcher;
import com.unimas.gatherdata.gather.Record;
import com.unimas.gatherdata.gather.Registry;
//import LocalLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
//import java.util.logging.Logger;
import com.unimas.gatherdata.output.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件采集单个文件不可大于(Integer.MAX_VALUE - 8)字节;
 */
public class Gather {

    //    private final Logger logger = LocalLog.getLogger();
    private final Logger logger = LoggerFactory.getLogger(Gather.class);

    private final int threads;
    private final long interval;
    private final String paths_;
    private final Output output;

    private ScheduledExecutorService scheduledService;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService registryService;
    private ScheduledFuture<?> registryFuture;
    private ExecutorService executor;

    private ConcurrentHashMap<String, String> cache;

    Gather(int threads, int interval, String path, String... outMsg) {
        this.threads = threads;
        this.interval = interval;
        this.paths_ = path;
        this.output = Output.getOutput(outMsg);
    }

    public void gather() {
        List<Path> pathList = new ArrayList<>();
        this.resolvePaths(pathList);
        if (pathList.isEmpty()) {
            logger.error(" gather path is null or empty...");
            return;
        }
        cache = new ConcurrentHashMap<>(pathList.size());
        logger.info("=================gather start=================");
        logger.debug("threads:" + threads + "-interval:" + interval + "-path:" +
                Arrays.toString(pathList.toArray()));
        Map<String, String> initCache = Registry.get();
        if (!initCache.isEmpty()) cache.putAll(initCache);
        logger.debug("init cache size:" + cache.size());
        scheduledService = Executors.newScheduledThreadPool(1);
        registryService = Executors.newScheduledThreadPool(1);
        executor = Executors.newFixedThreadPool(threads);
        scheduledFuture = scheduledService.scheduleWithFixedDelay(
                new GatherImpl(executor, pathList), 1, interval, TimeUnit.SECONDS);
        registryFuture = registryService.scheduleWithFixedDelay(() -> Registry.write(cache),
                1, 60, TimeUnit.SECONDS);
    }

    public void close() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) { //ignore
            }
            executor.shutdownNow();
        }
        if (registryFuture != null) registryFuture.cancel(true);
        if (registryService != null) {
            registryService.shutdown();
            try {
                registryService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {//ignore
            }
            registryService.shutdownNow();
        }
        if (scheduledFuture != null) scheduledFuture.cancel(true);
        if (scheduledService != null) {
            scheduledService.shutdown();
            try {
                scheduledService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {//ignore
            }
            scheduledService.shutdownNow();
        }
        output.close();
        logger.info("=================gather stop=================");
    }

    private void resolvePaths(List<Path> pathList) {
        String[] s_paths = paths_.split(",");
        for (String s_path : s_paths) {
            Path path = Paths.get(s_path);
            File file = path.toFile();
            if (!file.exists()) {
                logger.warn(path + " does not exit...");
                continue;
            }
            if (file.isFile()) {
                pathList.add(path);
                continue;
            }
            if (file.isDirectory()) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path p : stream) if (p.toFile().isFile()) pathList.add(p);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private class GatherImpl implements Runnable {

        private ExecutorService executor;
        private List<Path> paths;

        private GatherImpl(ExecutorService executor, List<Path> paths) {
            this.executor = executor;
            this.paths = paths;
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
            logger.debug("once gather start...");
            List<Future<Record>> results = new ArrayList<>(paths.size());
            for (Path path : paths) {
                if (path.toFile().exists()) results.add(executor.submit(new FileWatcher(path)));
                else {
                    logger.warn(path + " does not exit...");
                    cache.remove(path.toString());
                }
            }
            for (Future<Record> r : results) {
                try {
                    Record record = r.get();
                    String value = record.getLastTime();
                    if (!value.isEmpty()) {
                        String key = record.getPath().toString();
                        if (!cache.containsKey(key) || !value.equals(cache.get(key))) {
                            cache.put(key, value);
                            try {
                                output.apply(key, Files.readAllBytes(Paths.get(key)));
                            } catch (IOException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.debug("once gather finish...");
        }
    }
}
