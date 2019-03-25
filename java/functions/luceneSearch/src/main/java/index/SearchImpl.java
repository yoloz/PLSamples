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
import org.nutz.ssdb4j.SSDBs;
import org.nutz.ssdb4j.spi.Response;
import org.nutz.ssdb4j.spi.SSDB;
import util.Constants;
import util.JsonUtil;
import util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SearchImpl {

    private final Logger logger = Logger.getLogger(SearchImpl.class);

    private String key;
    private int start;
    private int rowCount;

    //following parameters need by select sql
    private Query query;
    private int nrtLimit;     //need by nrt search
    private List<String> cols;
    private String indexName;  //also need by to pull source data
    private Source source;     //also need by to pull source data


    //"select xx" or [key,offset,limit]
    SearchImpl(Object... params) {
        this.key = (String) params[0];
        this.rowCount = params[2] == null ? 0 : (int) params[2];
        this.start = params[1] == null ? 0 : ((int) params[1]) * rowCount;
    }

    Map<String, Object> search() throws IOException, LSException {
        Map<String, Object> results = indexSearch();
        if (indexName == null) indexName = Searcher.mapper.get(key);
        if (indexName == null) throw new LSException("get data from index by[" + key + "] that indexName is null");
        source = Utils.getSchema(indexName).getSource();
        if (Source.Type.LIST == source.getType() || Source.Type.HASH == source.getType()) fromSsdb(results);
        else throw new LSException("源类型[" + source.getType() + "]暂不支持");
        return results;
    }

    @SuppressWarnings("unchecked")
    private void fromSsdb(Map<String, Object> results) throws IOException {
        List<String> cols = (List<String>) results.remove("cols");
        List<Pair<String, Object>> _datas = (List<Pair<String, Object>>) results.remove("list");
        List<Map<String, Object>> dm = new ArrayList<>(_datas.size());
        try (SSDB ssdb = SSDBs.simple(source.getIp(), source.getPort(), SSDBs.DEFAULT_TIMEOUT)) {
            Response response;
            for (Pair<String, Object> pair : _datas) {
                if (Source.Type.HASH == source.getType())
                    response = ssdb.hget(pair.getLeft(), pair.getRight());
                else response = ssdb.qget(pair.getLeft(), (int) pair.getRight());
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

    private Map<String, Object> indexSearch() throws LSException, IOException {
        String prefix = Utils.trimPrefix(key).substring(0, 6).toLowerCase();
        if ("select".equals(prefix)) {
            String sql = key;
            SelectSql selectSql = new SelectSql(sql);
            Pair<Integer, Integer> _limit = selectSql.getLimit();
            start = _limit.getLeft();
            rowCount = _limit.getRight();
            indexName = selectSql.getIndexName();
            source = selectSql.getSchema().getSource();
            query = selectSql.getQuery();
            cols = selectSql.getSelects();

            IndexImpl indexImpl = Indexer.indexes.getIfPresent(indexName);
            if (indexImpl == null) {
                logger.debug("索引[" + indexName + "]非运行中,IndexReader查询");
                return offSearch();
            }

            logger.debug("索引[" + indexName + "]运行中,近实时查询");
            key = Utils.md5(sql);
            nrtLimit = Constants.pageCache * rowCount;
//            if (Searcher.searches.getIfPresent(key) != null) {
//                logger.warn("清空索引[" + indexName + "]查询[" + sql + "]缓存,重新查询");
//                Searcher.searches.invalidate(key);
//            }
            return nrtSearch(key, indexImpl.getSearcher());
        }
        logger.debug("取缓存[" + key + "]分页数据");
        Triple<List<String>, List<Pair<String, Object>>, Integer> triple = Searcher.searches.getIfPresent(key);
        if (triple == null) throw new LSException("缓存已经移除,请重新查询");
        List<Pair<String, Object>> cache = triple.getMiddle();
        //返回total<=pageCache*pageSize,下面错误理论不会出现
        if (cache.size() < start) throw new LSException("请求数据越界[start > cacheSize]");
        List<Object> list = new ArrayList<>(rowCount);
        int end = Math.min(cache.size(), start + rowCount);
        for (int i = start; i < end; i++) list.add(cache.get(i));
        Map<String, Object> results = new HashMap<>(3);
        results.put("total", triple.getRight());
        results.put("list", list);
        results.put("cols", triple.getLeft());
        return results;
    }

    /**
     * @return {"total":,"list":[<pullName,key>...],"cols":[f1,f2...]}
     * @throws IOException io exception
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> offSearch() throws IOException {
        Path indexPath = Constants.indexDir.resolve(indexName);
        Map<String, Object> results = new HashMap<>(3);
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

    /**
     * @param key      sqlId
     * @param searcher searcher{@link IndexSearcher}
     * @return {"total":,"size":,"key":"","list":[<pullName,key>...],"cols":[f1,f2...]}
     * @throws IOException io exception
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> nrtSearch(String key, IndexSearcher searcher) throws IOException {
        Map<String, Object> results = new HashMap<>(5);
        ScoreDoc scoreDoc = this.firstSearch(searcher, results);
        if (!results.isEmpty()) {
            int totalHits = (int) results.get("total");
            List<Pair<String, Object>> list = (List<Pair<String, Object>>) results.remove("list");
            int total = Math.min(nrtLimit, totalHits);
            Searcher.searches.put(key, Triple.of(cols, list, total));
            Searcher.mapper.put(key, indexName);
            if (list.size() < nrtLimit && list.size() < totalHits)
                new AfterSearch(searcher, key, scoreDoc, list.size()).start();
            List<Pair<String, Object>> _l = new ArrayList<>(rowCount);
            for (int i = start; i < list.size(); i++) {
                _l.add(list.get(i));
            }
            results.put("list", _l);
            results.put("size", total);
            results.put("key", key);
            results.put("cols", cols);
        } else {
            results.put("total", 0);
            results.put("size", 0);
            results.put("list", Collections.emptyList());
            results.put("key", key);
            results.put("cols", cols);
        }
        return results;
    }

    private ScoreDoc firstSearch(IndexSearcher searcher, Map<String, Object> results) throws IOException {
        int limit = start + rowCount;
        TopDocs topDocs = searcher.search(query, limit);
        ScoreDoc[] hits = topDocs.scoreDocs;
        int totalHits = Math.toIntExact(topDocs.totalHits);
        logger.debug("first search[" + indexName + "] total[" + totalHits + "] matching documents");
        int end = Math.min(totalHits, limit);
        if (end < totalHits && end > hits.length) {
            logger.debug("top hits[" + hits.length + "] of total[" + totalHits
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
                    logger.debug("backstage search[" + indexName + "] total[" + totalHits +
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
                logger.error("index[" + indexName + "] backstage search error", e);
            }
        }
    }
}
