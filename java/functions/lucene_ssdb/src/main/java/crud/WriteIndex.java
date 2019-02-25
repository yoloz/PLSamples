package crud;


import bean.Constants;
import bean.LSException;
import bean.Schema;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.nutz.ssdb4j.spi.SSDB;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;


class WriteIndex {

    private Logger logger = Logger.getLogger(WriteIndex.class);

    private Schema schema;
    private String indexPath = Constants.indexDir;

    private SSDB ssdb;


    WriteIndex(Schema schema) throws LSException {
        this.schema = schema;
    }

//    private void initSchema() throws LSException {
//        try (InputStream inputStream = Files.newInputStream(Paths.get(Constants.appDir,
//                indexName + ".yaml"))) {
//            schema = new Yaml().loadAs(inputStream, Schema.class);
//        } catch (Exception e) {
//            throw new LSException("初始化index[" + indexName + "]错误", e);
//        }
//    }

    void write() throws LSException {
        String usage = "java org.apache.lucene.demo.crud.WriteIndex"
                + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                + "in INDEX_PATH that can be searched with crud.SearchFiles";
        String docsPath = null;
        boolean create = true;

        Date start = new Date();
        try {
            logger.debug("indexing[" + schema.getIndex() + "] to '" + indexPath + "'");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = Class.forName(schema.getAnalyser());
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
           /*  Optional: for better indexing performance, if you
             are indexing many documents, increase the RAM
             buffer.  But if you do this, increase the max heap
             size to the JVM (eg add -Xmx512m or -Xmx1g):*/
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            this.indexSsdb(writer, docDir);

           /*  NOTE: if you want to maximize search performance,
             you can optionally call forceMerge here.  This can be
             a terribly costly operation, so generally it's only
             worth it when your index is relatively static (ie
             you're done adding documents to it):*/
            // writer.forceMerge(1);

            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime() + " total milliseconds");

        } catch (IOException | ClassNotFoundException e) {
            throw new LSException("写索引出错", e);
        }
    }

    private void indexSsdb(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            // make a new, empty document
            Document doc = new Document();

            // Add the path of the file as a field named "path".  Use a
            // field that is indexed (i.e. searchable), but don't tokenize
            // the field into separate words and don't index term frequency
            // or positional information:
            Field pathField = new StringField("path", file.toString(), Field.Store.YES);
            doc.add(pathField);

            // Add the last modified date of the file a field named "modified".
            // Use a LongPoint that is indexed (i.e. efficiently filterable with
            // PointRangeQuery).  This indexes to milli-second resolution, which
            // is often too fine.  You could instead create a number based on
            // year/month/day/hour/minutes/seconds, down the resolution you require.
            // For example the long value 2011021714 would mean
            // February 17, 2011, 2-3 PM.
            doc.add(new LongPoint("modified", lastModified));

            // Add the contents of the file to a field named "contents".  Specify a Reader,
            // so that the text of the file is tokenized and indexed, but not stored.
            // Note that FileReader expects the file to be in UTF-8 encoding.
            // If that's not the case searching for special characters will fail.
            doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

            if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                // New index, so we just add the document (no old document can be there):
                System.out.println("adding " + file);
                writer.addDocument(doc);
            } else {
                // Existing index (an old copy of this document may have been indexed) so
                // we use updateDocument instead to replace the old one matching the exact
                // path, if present:
                System.out.println("updating " + file);
                writer.updateDocument(new Term("path", file.toString()), doc);
            }
        }
    }
}
