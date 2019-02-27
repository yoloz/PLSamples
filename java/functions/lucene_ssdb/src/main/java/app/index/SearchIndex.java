package app.index;

import bean.Field;
import bean.ImmutablePair;
import bean.LSException;
import bean.Schema;
import bean.Ssdb;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import util.Constants;
import util.JsonUtil;
import util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SearchIndex {

    private final Logger logger = Logger.getLogger(SearchIndex.class);

    private final Path indexPath;
    private final Schema schema;

    public SearchIndex(Schema schema) {
        this.schema = schema;
        this.indexPath = Constants.indexDir.resolve(schema.getIndex());
    }

    /**
     * @param pair  pair key:field,value:condition
     * @param limit query count
     * @return list {@link List<String>}
     * @throws LSException error
     */
    public List<Map<String, Object>> search(ImmutablePair<String, String> pair, int limit) throws LSException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = Utils.getInstance(schema.getAnalyser(), Analyzer.class);
            QueryParser parser = new QueryParser(pair.getLeft(), analyzer);
            Query query = parser.parse(pair.getRight());
            logger.debug("Searching for: " + query.toString(pair.getLeft()));
            TopDocs results = searcher.search(query, limit);
            ScoreDoc[] hits = results.scoreDocs;
            int numTotalHits = Math.toIntExact(results.totalHits);
            logger.debug(numTotalHits + " total matching documents");
            int end = Math.min(numTotalHits, limit);
            if (end > hits.length) {
                logger.warn("top hits[" + hits.length + "] of total[" + numTotalHits
                        + "] matched documents collected,need collect more[" + end + "]");
                hits = searcher.search(query, numTotalHits).scoreDocs;
            }
            end = Math.min(hits.length, limit);
            for (int i = 0; i < end; i++) {
                Map<String, Object> map = new HashMap<>(schema.getFields().size());
                Document doc = searcher.doc(hits[i].doc);
                for (Field f : schema.getFields()) {
                    String name = f.getName();
                    if (doc.get(name) != null) switch (f.getType()) {
                        case INT:
                            map.put(name, Integer.valueOf(doc.get(name)));
                            break;
                        case LONG:
                            map.put(name, Long.valueOf(doc.get(name)));
                            break;
                        case TEXT:
                        case STRING:
                            map.put(name, doc.get(name));
                            break;
                        case DATE:
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(f.getFormatter());
                            long mills = Long.valueOf(doc.get(name));
                            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(mills),
                                    ZoneOffset.UTC);
                            map.put(name, time.format(formatter));
                            break;
                    }
                }
                if (Ssdb.Type.LIST == schema.getSsdb().getType()) {
                    String _index = doc.get("_index");
                    if (_index != null) map.put("_index", Integer.valueOf(_index));
                } else if (Ssdb.Type.HASH == schema.getSsdb().getType()) {
                    String _key = doc.get("_key");
                    if (_key != null) map.put("_key", _key);
                }
                list.add(map);
            }
        } catch (ParseException | IOException e) {
            throw new LSException("query[" + pair.getRight() + "] error", e);
        }
        return list;
    }
}
