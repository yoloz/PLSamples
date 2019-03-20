package index;

import bean.Triple;
import bean.LSException;
import bean.Schema;
import bean.Ssdb;
import index.pull.Pull;
import index.pull.SsdbPull;
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
import util.SqlliteUtil;
import util.Utils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 索引原始数据均不存储
 */
public class IndexImpl implements Runnable, Closeable {

    private final Logger logger;

    private IndexWriter indexWriter;
    private SearcherManager searcherManager;
    //<pullName,key,value>
    private final ArrayBlockingQueue<Triple<String, Object, Object>> queue =
            new ArrayBlockingQueue<>(1000);

    private final int blockSec = 10;

    private final Schema schema;
    private final Path indexPath;
    private Pull pull;


    public IndexImpl(Schema schema, Logger logger) {
        this.schema = schema;
        this.indexPath = Constants.indexDir.resolve(schema.getIndex());
        this.logger = logger;
        if (schema.getSsdb() != null)
            this.pull = new SsdbPull(schema.getSsdb(), schema.getIndex(), queue, blockSec, logger);
    }

    @Override
    public void run() {
        try {
            logger.info("start index[" + schema.getIndex() + "]");
            this.initIndex();
            new Thread(pull).start();
            this.indexImpl();
            //pull stop that index writer will stop and need commit
            logger.info("committing index[" + schema.getIndex() + "]");
            //this.indexWriter.forceMerge(1);
            this.indexWriter.commit();
            this.indexWriter.close();
        } catch (IOException | LSException e) {
            logger.error("index[" + schema.getIndex() + "] error", e);
            this.close();
        }
    }


    @Override
    public void close() {
        logger.info("close index[" + schema.getIndex() + "]...");
        if (this.pull != null) this.pull.close();
        try {
            this.searcherManager.close();
            if (this.indexWriter != null && this.indexWriter.isOpen()) {
                logger.info("committing index[" + schema.getIndex() + "]");
                //this.indexWriter.forceMerge(1);
                this.indexWriter.commit();
                this.indexWriter.close();
            }
        } catch (IOException e) {
            logger.error("close[" + schema.getIndex() + "] error", e);
        }
        this.searcherManager = null;
    }

    public IndexSearcher getSearcher() throws IOException {
        if (this.searcherManager != null) return this.searcherManager.acquire();
        else return null;
    }

    private void initIndex() throws IOException, LSException {
        Directory dir = FSDirectory.open(indexPath);
        Analyzer analyzer = Utils.getInstance(schema.getAnalyser(), Analyzer.class);
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        iwc.setRAMBufferSizeMB(128.0);
        this.indexWriter = new IndexWriter(dir, iwc);
        this.searcherManager = new SearcherManager(indexWriter, false,
                false, null);
        ControlledRealTimeReopenThread<IndexSearcher> crtThread =
                new ControlledRealTimeReopenThread<>(indexWriter, searcherManager,
                        300.0, 0.5);
        crtThread.setDaemon(true);
        crtThread.setName("update-" + schema.getIndex());
        crtThread.start();
    }

    private void indexImpl() throws IOException {
        int counter = 0;
        while (pull.isRunning()) {
            try {
                Triple<String, Object, Object> triple = this.queue.poll(blockSec, TimeUnit.SECONDS);
                if (triple == null) continue;
//                Object rv = triple.getRight();
//                if (!(rv instanceof String)) {
//                    logger.warn("暂时仅支持处理字符串值!");
//                }
                Map<String, Object> data;
                try {
                    data = JsonUtil.toMap((String) triple.getRight());
                } catch (IOException e) {
                    logger.warn("ssdb[" + triple.getLeft() + "," + triple.getMiddle()
                            + "]value convert to JSON fail,this record will be discarded...");
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
                                break;
                            case DATE:
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(field.getFormatter());
                                LocalDateTime localDateTime = LocalDateTime.parse(String.valueOf(value), formatter);
                                doc.add(new LongPoint(name, Long.valueOf(Utils.toNanos(localDateTime))));
                                break;
                            case LONG:
                                long l;
                                if (value instanceof Long) l = (long) value;
                                else l = Long.valueOf(String.valueOf(value));
                                doc.add(new LongPoint(name, l));
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
                if (Ssdb.Type.LIST == schema.getSsdb().getType()) {
                    doc.add(new StoredField("_index", (int) triple.getMiddle()));
                } else {
                    doc.add(new StoredField("_key", (String) triple.getMiddle()));
                }
                doc.add(new StoredField("_name", triple.getLeft()));
                indexWriter.addDocument(doc);

                counter++;
                if (counter >= 10000) {
                    logger.debug("update [" + schema.getIndex() + "] point message");
                    SqlliteUtil.update("update point set name=?,value=? where iname=?",
                            triple.getLeft(), triple.getMiddle(), schema.getIndex());
                    counter = 0;
                }
            } catch (InterruptedException e) {
                logger.error(schema.getIndex() + " queue error", e);
            } catch (SQLException e) {
                logger.error(schema.getIndex() + " update point error", e);
            }

        }
    }

}
