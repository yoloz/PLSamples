package index;

import bean.LSException;
import bean.Pair;
import bean.Source;
import bean.Triple;
import index.parse.SelectSql;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import util.Constants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SearchImpl {

    private final Logger logger = Logger.getLogger(SearchImpl.class);

    private final int nrtLimit;

    private final int start;
    private final int rowCount;
    private final Source source;

    private final Query query;
    private final String indexName;
    private List<String> cols;


    SearchImpl(SelectSql selectSql) throws LSException {
        this(selectSql.getIndexName(), selectSql.getSchema().getSource(), selectSql.getQuery(),
                selectSql.getLimit().getLeft(), selectSql.getLimit().getRight(), selectSql.getSelects());
    }

    private SearchImpl(String indexName, Source source, Query query, int start, int rowCount, List<String> cols) {
        this.start = start;
        this.rowCount = rowCount;
        this.source = source;
        this.query = query;
        this.indexName = indexName;
        this.cols = cols;
        this.nrtLimit = Constants.pageCache * rowCount;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> offSearch() throws IOException {
        Path indexPath = Constants.indexDir.resolve(indexName);
        Map<String, Object> results = new HashMap<>(2);
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
            IndexSearcher searcher = new IndexSearcher(reader);
            this.firstSearch(searcher, results);
        }
        if (!results.isEmpty()) {
            List<Pair<String, Object>> list = (List<Pair<String, Object>>) results.remove("list");
            List<Pair<String, Object>> _l = new ArrayList<>(rowCount);
            for (int i = start; i < list.size(); i++) {
                _l.add(list.get(i));
            }
            results.put("list", _l);
        } else {
            results.put("total", 0);
            results.put("list", Collections.emptyList());
        }
        results.put("cols", cols);
        return results;
    }

    @SuppressWarnings("unchecked")
    void nrtSearch(String key, IndexSearcher searcher) throws IOException {
        Map<String, Object> first = new HashMap<>(2);
        ScoreDoc scoreDoc = this.firstSearch(searcher, first);
        if (!first.isEmpty()) {
            int totalHits = (int) first.get("total");
            List<Pair<String, Object>> list = (List<Pair<String, Object>>) first.remove("list");
            int total = Math.min(nrtLimit, totalHits);
            Searcher.searches.put(key, Triple.of(cols, list, total));
            if (list.size() < nrtLimit && list.size() < totalHits)
                new AfterSearch(searcher, key, scoreDoc, list.size()).start();
        }
    }

    private ScoreDoc firstSearch(IndexSearcher searcher, Map<String, Object> results) throws IOException {
        int limit = start + rowCount;
        TopDocs topDocs = searcher.search(query, limit);
        ScoreDoc[] hits = topDocs.scoreDocs;
        int totalHits = Math.toIntExact(topDocs.totalHits);
        logger.info("first search[" + indexName + "] total[" + totalHits + "] matching documents");
        int end = Math.min(totalHits, limit);
        if (end < totalHits && end > hits.length) {
            logger.warn("top hits[" + hits.length + "] of total[" + totalHits
                    + "] matched documents collected,need collect more[" + end + "]");
            hits = searcher.search(query, end).scoreDocs;
        }
        List<Pair<String, Object>> list = new ArrayList<>(hits.length);
        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);
            String pullName = doc.get("_name");
            String key = doc.get("_key");
            if (pullName == null || key == null) logger.warn("search [_name] is null or [_key] is null");
            else {
                if (Source.Type.LIST == source.getType()) list.add(Pair.of(pullName, Integer.valueOf(key)));
                else list.add(Pair.of(pullName, key));
            }
        }
        results.put("total", totalHits);
        results.put("list", list);
        return hits[hits.length - 1];
    }

    private class AfterSearch extends Thread {


        private final IndexSearcher searcher;
        private final String key;
        private int afterCount;
        private ScoreDoc scoreDoc;

        private AfterSearch(IndexSearcher searcher, String key, ScoreDoc scoreDoc, int firstCount) {
            this.scoreDoc = scoreDoc;
            this.key = key;
            this.searcher = searcher;
            this.afterCount = nrtLimit - firstCount;
        }

        @Override
        public void run() {
            try {
                Triple<List<String>, List<Pair<String, Object>>, Integer> value = Searcher.searches.getIfPresent(key);
                if (value == null) throw new IOException("search[" + key + "]不存在");
                List<Pair<String, Object>> cache = value.getMiddle();
                while (afterCount > 0) {
                    TopDocs topDocs = searcher.searchAfter(scoreDoc, query, afterCount);
                    ScoreDoc[] hits = topDocs.scoreDocs;
                    int totalHits = Math.toIntExact(topDocs.totalHits);
                    logger.info("backstage search[" + indexName + "] total[" + totalHits +
                            "] need[" + afterCount + "]");
                    for (ScoreDoc hit : hits) {
                        Document doc = searcher.doc(hit.doc);
                        String pullName = doc.get("_name");
                        String key = doc.get("_key");
                        if (pullName == null || key == null)
                            logger.warn("search [_name] is null or [_key] is null");
                        else {
                            if (Source.Type.LIST == source.getType())
                                cache.add(Pair.of(pullName, Integer.valueOf(key)));
                            else cache.add(Pair.of(pullName, key));
                        }
                    }
                    afterCount -= hits.length;
                    scoreDoc = hits[hits.length - 1];
                }
            } catch (IOException e) {
                logger.warn("index[" + indexName + "] search error", e);
            }
        }
    }
}
