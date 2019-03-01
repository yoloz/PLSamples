package app.index;


import org.apache.lucene.document.*;
import util.Constants;
import bean.ImmutablePair;
import bean.LSException;
import bean.Schema;
import bean.Ssdb;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import util.JsonUtil;
import app.source.SsdbPull;
import util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * setRAMBufferSizeMB:
 * <p>
 * Optional: for better indexing performance, if you
 * are indexing many documents, increase the RAM
 * buffer.  But if you do this, increase the max heap
 * size to the JVM (eg add -Xmx512m or -Xmx1g).
 * <p>
 * forceMerge:
 * <p>
 * NOTE: if you want to maximize search performance,
 * you can optionally call forceMerge here.  This can be
 * a terribly costly operation, so generally it's only
 * worth it when your index is relatively static (ie
 * you're done adding documents to it).
 */
public class WriteIndex extends Thread {

    private Logger logger = Logger.getLogger(WriteIndex.class);

    private final Schema schema;
    private final CountDownLatch countDownLatch;

    private final Path indexPath;

    public WriteIndex(Schema schema, CountDownLatch countDownLatch) {
        this.schema = schema;
        this.countDownLatch = countDownLatch;
        this.indexPath = Constants.indexDir.resolve(schema.getIndex());
    }

    private void write() throws LSException {
        try {
            Directory dir = FSDirectory.open(indexPath);
            Analyzer analyzer = Utils.getInstance(schema.getAnalyser(), Analyzer.class);
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            iwc.setRAMBufferSizeMB(256.0);
            IndexWriter writer = new IndexWriter(dir, iwc);
            if (schema.getSsdb() != null) this.indexSsdb(writer);
//             writer.forceMerge(1);
            logger.debug("committing index[" + schema.getIndex() + "]");
            long start = System.currentTimeMillis();
            writer.commit();
            writer.close();
            long end = System.currentTimeMillis();
            logger.debug("index[" + schema.getIndex() + "] commit cost time[" + (end - start) + "] mills");
        } catch (IOException e) {
            throw new LSException("写索引出错", e);
        }
    }

    private void indexSsdb(IndexWriter writer) throws LSException {
        Ssdb ssdb = schema.getSsdb();
        String addr = ssdb.getAddr();
        int idex = addr.indexOf(":");
        SsdbPull ssdbPull = new SsdbPull(addr.substring(0, idex),
                Integer.parseInt(addr.substring(idex + 1)),
                ssdb.getName(), ssdb.getType(), schema.getIndex());
        ssdbPull.start();
        logger.debug("indexing[" + schema.getIndex() + "]");
        long start = System.currentTimeMillis();
        long count = 0;
        while (true) {
            try {
                ImmutablePair<Object, String> pair = ssdbPull.queue.poll(ssdbPull.timeout, TimeUnit.MILLISECONDS);
                if (pair == null) break;
                Map<String, Object> data;
                try {
                    data = JsonUtil.toMap(pair.getRight());
                } catch (IOException e) {
                    logger.warn("ssdb." + ssdb.getName()
                            + " value[" + pair.getRight() + "] format is not support,this record will be discarded...");
                    data = null;
                }
                if (data == null) continue;
                Document doc = new Document();
                for (bean.Field field : schema.getFields()) {
                    String name = field.getName();
                    if (data.containsKey(name)) {
                        switch (field.getType()) {
                            case INT:
                                int i = (int) data.get(name);
                                doc.add(new IntPoint(name, i));
                                doc.add(new NumericDocValuesField(name, i));
                                doc.add(new StoredField(name, i));
                                break;
                            case DATE:
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(field.getFormatter());
                                LocalDateTime localDateTime = LocalDateTime.parse(String.valueOf(data.get(name)), formatter);
                                doc.add(new StringField(name, Utils.toNanos(localDateTime), Field.Store.YES));
//                                doc.add(new LongPoint(name, mills));
//                                doc.add(new NumericDocValuesField(name, mills));
//                                doc.add(new StoredField(name, mills));
                                break;
                            case LONG:
                                long l = (long) data.get(name);
                                doc.add(new LongPoint(name, l));
                                doc.add(new NumericDocValuesField(name, l));
                                doc.add(new StoredField(name, l));
                                break;
                            case STRING:
                                doc.add(new StringField(name, String.valueOf(data.get(name)), Field.Store.YES));
                                break;
                            case TEXT:
                                doc.add(new TextField(name, String.valueOf(data.get(name)), Field.Store.YES));
                                break;
                        }
                    }
                }
                if (Ssdb.Type.LIST == ssdb.getType()) {
                    doc.add(new StoredField("_index", (int) pair.getLeft()));
                } else if (Ssdb.Type.HASH == ssdb.getType()) {
                    doc.add(new StoredField("_key", (String) pair.getLeft()));
                }
                writer.addDocument(doc);
                count++;
            } catch (InterruptedException e) {
                logger.warn("队列中断", e);
            } catch (DateTimeParseException | IOException e) {
                throw new LSException("索引[" + schema.getIndex() + "]创建document出错", e);
            }
        }
        long end = System.currentTimeMillis();
        logger.debug("index [" + schema.getIndex() + "]finished,count[" + count + "],cost time[" + (end - start) + "] mills");
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
            this.write();
        } catch (Exception e) {
            countDownLatch.countDown();
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
        }
        countDownLatch.countDown();
    }
}
