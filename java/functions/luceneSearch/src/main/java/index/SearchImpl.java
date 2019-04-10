package index;

import bean.LSException;
import bean.Pair;
import bean.Source;
import index.parse.SelectSql;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.Response;
import org.nutz.ssdb4j.spi.SSDB;
import util.Constants;
import util.JsonUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SearchImpl {

    private final Logger logger = Logger.getLogger(SearchImpl.class);

    private final String sql;
    private long token;   //分页searcher


    SearchImpl(String sql, long token) {
        this.sql = sql;
        this.token = token;
    }

    @SuppressWarnings("unchecked")
    private void fromSsdb(Map<String, Object> results, List<String> cols, Source source) throws IOException {
        List<Pair<String, String>> _datas = (List<Pair<String, String>>) results.remove("list");
        List<Map<String, Object>> dm = new ArrayList<>(_datas.size());
        try (SSDB ssdb = SSDBs.simple(source.getIp(), source.getPort(), SSDBs.DEFAULT_TIMEOUT)) {
            Response response;
            for (Pair<String, String> pair : _datas) {
                if (Source.Type.HASH == source.getType())
                    response = ssdb.hget(pair.getLeft(), pair.getRight());
                else if (Source.Type.LIST == source.getType())
                    response = ssdb.qget(pair.getLeft(), Integer.parseInt(pair.getRight()));
                else throw new IOException("type[" + source.getType() + "] is not support");
                if (response.datas.size() > 0) {
                    Map<String, Object> map = new HashMap<>(cols.size());
                    String value = new String(response.datas.get(0), SSDBs.DEFAULT_CHARSET);
                    Map<String, Object> _m = JsonUtil.toMap(value);
                    for (String col : cols) if (_m.containsKey(col)) map.put(col, _m.get(col));
                    dm.add(map);
                }
            }
        }
        results.put("results", dm);
    }

    Map<String, Object> search() throws LSException, IOException {
        Map<String, Object> results;
        SelectSql selectSql = new SelectSql(sql);
        Pair<Integer, Integer> _limit = selectSql.getLimit();
        int offset = _limit.getLeft();
        int rowCount = _limit.getRight();
        String indexName = selectSql.getIndexName();
        Source source = selectSql.getSchema().getSource();
        Query query = selectSql.getQuery();
        List<String> cols = selectSql.getSelects();
        Sort sort = selectSql.getOrder();
        logger.debug("query[" + query + "],order[" + sort + "]");
        IndexImpl indexImpl = Indexer.indexes.getIfPresent(indexName);
        if (indexImpl == null) {
            logger.debug("索引[" + indexName + "]非运行中,IndexReader查询");
            results = offSearch(indexName, query, sort, offset, rowCount);
        } else {
            logger.debug("索引[" + indexName + "]运行中,近实时查询");
            Pair<Long, IndexSearcher> searcher = indexImpl.getSearcher(token);
            results = nrtSearch(searcher.getRight(), query, sort, offset, rowCount);
            results.put("key", searcher.getLeft());
        }
        if (Source.Type.LIST == source.getType() || Source.Type.HASH == source.getType())
            fromSsdb(results, cols, source);
        else throw new LSException("源类型[" + source.getType() + "]暂不支持");
        return results;
    }

    /**
     * @return {"total":,"list":[<pullName,key>...]}
     * @throws IOException io exception
     */
    private Map<String, Object> offSearch(String indexName, Query query, Sort sort, int offset, int rowCount)
            throws IOException {
        Path indexPath = Constants.indexDir.resolve(indexName);
        Map<String, Object> results;
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
            IndexSearcher searcher = new IndexSearcher(reader);
            results = _search(searcher, query, sort, offset, rowCount);
        }
        return results;
    }

    /**
     * @return {"total":,"list":[<pullName,key>...]}
     * @throws IOException io exception
     */
    private Map<String, Object> nrtSearch(IndexSearcher searcher, Query query, Sort sort, int offset, int rowCount)
            throws IOException {
        Map<String, Object> results;
        try {
            results = _search(searcher, query, sort, offset, rowCount);
        } finally {
            searcher.getIndexReader().decRef();
        }
        return results;
    }

    /**
     * @param searcher index searcher
     * @return {"total","","list":[<pull,key>...]}
     * @throws IOException io error
     */
    private Map<String, Object> _search(IndexSearcher searcher, Query query, Sort sort, int offset, int rowCount)
            throws IOException {
        Map<String, Object> results = new HashMap<>(2);
        int prePage = (offset - 1) * rowCount;
        int curPage = offset * rowCount;
        int _count = 0;
        TopDocs topDocs;
        ScoreDoc scoreDoc = null;
        do {
            int n = 1000;
            if (curPage <= n || (offset - 1) * rowCount == 0) n = curPage;
            else {
                if (prePage <= 0) n = rowCount;
                else {
                    if (prePage <= n) n = prePage;
                    prePage -= 1000;
                }
            }
            if (sort == null) topDocs = searcher.searchAfter(scoreDoc, query, n);
            else topDocs = searcher.searchAfter(scoreDoc, query, n, sort);
            ScoreDoc[] hits = topDocs.scoreDocs;
            if (hits.length > 0) scoreDoc = hits[hits.length - 1];
            _count += hits.length;
            logger.debug("once search get hits[" + hits.length + "]");
            if (_count == topDocs.totalHits) break;
        } while (_count < curPage);
        ScoreDoc[] hits = topDocs.scoreDocs;
        long totalHits = topDocs.totalHits;
        List<Pair<String, String>> list = new ArrayList<>(rowCount);
        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);
            String pullName = doc.get("_name");
            String key = doc.get("_key");
            if (pullName == null || key == null) logger.warn("search [_name] is null or [_key] is null");
            else list.add(Pair.of(pullName, key));
        }
        results.put("total", totalHits);
        results.put("list", list);
        return results;
    }

//    private class AfterSearch extends Thread {
//
//
//        private final IndexSearcher searcher;
//        private final String key;
//        private int afterCount;
//        private ScoreDoc scoreDoc;
//
//        private AfterSearch(IndexSearcher searcher, String key, ScoreDoc scoreDoc, int firstCount) {
//            this.scoreDoc = scoreDoc;
//            this.key = key;
//            this.searcher = searcher;
//            this.afterCount = nrtLimit - firstCount;
//        }
//
//        @Override
//        public void run() {
//            if (key == null || key.isEmpty()) {
//                logger.warn("key is null or empty,so no after search");
//                return;
//            }
//            try {
//                Triple<List<String>, List<Pair<String, Object>>, Integer> value = Searcher.searches.getIfPresent(key);
//                if (value == null) throw new IOException("search[" + key + "]不存在");
//                List<Pair<String, Object>> cache = value.getMiddle();
//                while (afterCount > 0) {
//                    TopDocs topDocs;
//                    if (sort == null) topDocs = searcher.searchAfter(scoreDoc, query, afterCount);
//                    else topDocs = searcher.searchAfter(scoreDoc, query, afterCount, sort);
//                    ScoreDoc[] hits = topDocs.scoreDocs;
//                    int totalHits = Math.toIntExact(topDocs.totalHits);
//                    logger.debug("backstage search[" + indexName + "] total[" + totalHits +
//                            "] need[" + afterCount + "]");
//                    for (ScoreDoc hit : hits) {
//                        Document doc = searcher.doc(hit.doc);
//                        String pullName = doc.get("_name");
//                        String key = doc.get("_key");
//                        if (pullName == null || key == null)
//                            logger.warn("search [_name] is null or [_key] is null");
//                        else {
//                            if (Source.Type.LIST == source.getType())
//                                cache.add(Pair.of(pullName, Integer.valueOf(key)));
//                            else cache.add(Pair.of(pullName, key));
//                        }
//                    }
//                    afterCount -= hits.length;
//                    scoreDoc = hits[hits.length - 1];
//                }
//            } catch (IOException e) {
//                logger.error("index[" + indexName + "] backstage search error", e);
//            } finally {
//                IndexImpl impl = Indexer.indexes.getIfPresent(indexName);
//                if (impl != null) impl.releaseSearcher(searcher);
//            }
//        }
//    }
}
