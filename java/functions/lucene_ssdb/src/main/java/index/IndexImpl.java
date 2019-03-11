package index;

import app.source.SsdbPull;
import bean.ImmutablePair;
import bean.LSException;
import bean.Schema;
import bean.Ssdb;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import util.Constants;
import util.JsonUtil;
import util.Utils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IndexImpl implements Runnable, Closeable {

    private final Logger logger;

    private IndexWriter indexWriter;
    //是线程安全的.第二个參数是是否在全部缓存清空后让search看到
    private SearcherManager searcherManager;
    private IndexSearcher indexSearcher;

    private final ArrayBlockingQueue<ImmutablePair<Object, String>> queue =
            new ArrayBlockingQueue<>(1000);

    private final Schema schema;
    private final Path indexPath;

    public IndexImpl(Schema schema, Logger logger) {
        this.schema = schema;
        this.indexPath = Constants.indexDir.resolve(schema.getIndex());
        this.logger = logger;
    }

    @Override
    public void run() {

    }


    @Override
    public void close() throws IOException {

    }

    public IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }

    private void initIndex() throws IOException, LSException {
        Directory dir = FSDirectory.open(indexPath);
        Analyzer analyzer = Utils.getInstance(schema.getAnalyser(), Analyzer.class);
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        iwc.setRAMBufferSizeMB(128.0);
        this.indexWriter = new IndexWriter(dir, iwc);
        this.searcherManager = new SearcherManager(indexWriter, false, false, null);
        ControlledRealTimeReopenThread<IndexSearcher> crtThread =
                new ControlledRealTimeReopenThread<>(
                        indexWriter, searcherManager, 300.0, 0.5);
        crtThread.setDaemon(true);
        crtThread.setName("update-" + schema.getIndex());
        crtThread.start();
    }

    public void write() {
        try {


            if (schema.getSsdb() != null) this.indexSsdb(indexWriter);
//             indexWriter.forceMerge(1);
            logger.debug("committing index[" + schema.getIndex() + "]");
            long start = System.currentTimeMillis();
            indexWriter.commit();
            indexWriter.close();
            long end = System.currentTimeMillis();
            logger.debug("index[" + schema.getIndex() + "] commit cost time[" + (end - start) + "] mills");
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            this.close();
        }
    }

    private void indexSsdb(IndexWriter writer) throws LSException {
        Ssdb ssdb = schema.getSsdb();
//        String addr = ssdb.getAddr();
//        int idex = addr.indexOf(":");
//        ssdbPull = new SsdbPull(addr.substring(0, idex),
//                Integer.parseInt(addr.substring(idex + 1)),
//                ssdb.getName(), ssdb.getType(), schema.getIndex());
        ssdbPull = new SsdbPull(ssdb.getIp(), ssdb.getPort(), ssdb.getName(), ssdb.getType(), schema.getIndex());
        ssdbPull.start();
        logger.debug("indexing[" + schema.getIndex() + "]");
//        long start = System.currentTimeMillis();
//        long count = 0;
        while (!stop) {
            try {
                ImmutablePair<Object, String> pair = ssdbPull.queue.poll(ssdbPull.timeout, TimeUnit.MILLISECONDS);
//                if (pair == null) break;
                if (pair == null) continue;
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
                        Object value = data.get(name);
                        switch (field.getType()) {
                            case INT:
                                int i;
                                if (value instanceof Integer) i = (int) value;
                                else i = Integer.valueOf(String.valueOf(value));
                                doc.add(new IntPoint(name, i));
//                                doc.add(new NumericDocValuesField(name, i));
//                                doc.add(new StoredField(name, i));
                                break;
                            case DATE:
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(field.getFormatter());
                                LocalDateTime localDateTime = LocalDateTime.parse(String.valueOf(value), formatter);
//                                doc.add(new StringField(name, Utils.toNanos(localDateTime), Field.Store.YES));
                                doc.add(new LongPoint(name, Long.valueOf(Utils.toNanos(localDateTime))));
//                                doc.add(new NumericDocValuesField(name, mills));
//                                doc.add(new StoredField(name, mills));
                                break;
                            case LONG:
                                long l;
                                if (value instanceof Long) l = (long) value;
                                else l = Long.valueOf(String.valueOf(value));
                                doc.add(new LongPoint(name, l));
//                                doc.add(new NumericDocValuesField(name, l));
//                                doc.add(new StoredField(name, l));
                                break;
                            case STRING:
                                doc.add(new StringField(name, String.valueOf(value), Field.Store.NO));
                                break;
                            case TEXT:
                                doc.add(new TextField(name, String.valueOf(value), Field.Store.NO));
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
//                count++;
            } catch (InterruptedException e) {
                logger.warn("队列中断", e);
            } catch (DateTimeParseException | IOException e) {
                throw new LSException("索引[" + schema.getIndex() + "]创建document出错", e);
            }
        }
//        long end = System.currentTimeMillis();
//        logger.debug("index [" + schema.getIndex() + "]finished,count[" + count + "],cost time[" + (end - start) + "] mills");
    }
}
