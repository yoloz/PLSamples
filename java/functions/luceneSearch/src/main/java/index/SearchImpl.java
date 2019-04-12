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
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.GroupingSearch;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
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
        String group = selectSql.getGroup();
        logger.debug("query[" + query + "],order[" + sort + "],group[" + group + "]");
        IndexImpl indexImpl = Indexer.indexes.getIfPresent(indexName);
        if (indexImpl == null) {
            logger.debug("索引[" + indexName + "]非运行中,IndexReader查询");
            results = offSearch(indexName, query, sort, group, offset, rowCount);
        } else {
            logger.debug("索引[" + indexName + "]运行中,近实时查询");
            Pair<Long, IndexSearcher> searcher = indexImpl.getSearcher(token);
            results = nrtSearch(searcher.getRight(), query, sort, group, offset, rowCount);
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
    private Map<String, Object> offSearch(String indexName, Query query, Sort sort, String group,
                                          int offset, int rowCount)
            throws IOException {
        Path indexPath = Constants.indexDir.resolve(indexName);
        Map<String, Object> results;
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {
            IndexSearcher searcher = new IndexSearcher(reader);
            if (group == null) results = querySearch(searcher, query, sort, offset, rowCount);
            else results = groupSearch(searcher, query, group, sort, offset, rowCount);
        }
        return results;
    }

    /**
     * @return {"total":,"list":[<pullName,key>...]}
     * @throws IOException io exception
     */
    private Map<String, Object> nrtSearch(IndexSearcher searcher, Query query, Sort sort, String group,
                                          int offset, int rowCount)
            throws IOException {
        Map<String, Object> results;
        try {
            if (group == null) results = querySearch(searcher, query, sort, offset, rowCount);
            else results = groupSearch(searcher, query, group, sort, offset, rowCount);
        } finally {
            searcher.getIndexReader().decRef();
        }
        logger.debug("searcher ref: " + searcher.getIndexReader().getRefCount());
        return results;
    }

    /**
     * 普通查询
     *
     * @param searcher index searcher
     * @return {"total","","list":[<pull,key>...]}
     * @throws IOException io error
     */
    private Map<String, Object> querySearch(IndexSearcher searcher, Query query, Sort sort, int offset, int rowCount)
            throws IOException {
        Map<String, Object> results = new HashMap<>(2);
        List<Pair<String, String>> list = new ArrayList<>(rowCount);

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
        addData(searcher, hits, list);
        results.put("total", totalHits);
        results.put("list", list);
        return results;
    }

    /**
     * 分组查询
     *
     * @param searcher index searcher
     * @return {"total","","list":[<pull,key>...]}
     * @throws IOException io error
     */
    private Map<String, Object> groupSearch(IndexSearcher searcher, Query query, String colName,
                                            Sort colSort, int offset, int rowCount) throws IOException {
        Map<String, Object> results = new HashMap<>(2);
        List<Pair<String, String>> list = new ArrayList<>(rowCount);

        TopGroups<BytesRef> topGroups = groupImpl(searcher, query, colName, colSort,
                0, rowCount, 0);
        int totalGroupCount = topGroups.totalGroupCount;
        int totalHitCount = topGroups.totalHitCount;
        if (totalHitCount > 0) {
            GroupDocs<BytesRef> group = topGroups.groups[0];
            long hits = group.totalHits;
            int start = (offset - 1) * rowCount;
            if (offset * rowCount <= hits) {
                logger.debug("first group total[" + hits + "] start[" + start + "] to[" + offset * rowCount + "]");
                if (start > 0) {
                    topGroups = groupImpl(searcher, query, colName, colSort,
                            start, rowCount, 0);
                    group = topGroups.groups[0];
                }
                addData(searcher, group.scoreDocs, list);
            } else {
                int groupOffset = 0;
                boolean first = true;
                int pending;
                do {
                    if (first && start < hits) {
                        topGroups = groupImpl(searcher, query, colName, colSort,
                                start, Math.toIntExact(hits), groupOffset);
                        group = topGroups.groups[0];
                        addData(searcher, group.scoreDocs, list);
                        first = false;
                    }
                    groupOffset += 1;
                    pending = rowCount - list.size();
                    logger.debug("group[" + groupOffset + "] pending[" + pending + "]");
                    if (pending == 0) break;
                    if (pending < 0) throw new IOException("group page logic error");
                    topGroups = groupImpl(searcher, query, colName, colSort,
                            0, pending, groupOffset);
                    group = topGroups.groups[0];
                    hits += group.totalHits;
                    if (start < hits) addData(searcher, group.scoreDocs, list);
                } while (groupOffset < totalGroupCount);
            }
        }
        results.put("total", totalHitCount);
        results.put("list", list);
        return results;
    }

    private void addData(IndexSearcher searcher, ScoreDoc[] scoreDocs, List<Pair<String, String>> list)
            throws IOException {
        for (ScoreDoc hit : scoreDocs) {
            Document doc = searcher.doc(hit.doc);
            String pullName = doc.get("_name");
            String key = doc.get("_key");
            if (pullName == null || key == null)
                logger.warn("search [_name] is null or [_key] is null");
            else list.add(Pair.of(pullName, key));
        }
    }

    private TopGroups<BytesRef> groupImpl(IndexSearcher indexSearcher, Query query,
                                          String colName, Sort sort,
                                          int start, int rowCount, int groupOffset) throws IOException {
        GroupingSearch groupingSearch = new GroupingSearch(colName);
        Sort groupSort = Sort.RELEVANCE;
        Sort withInGroup = Sort.RELEVANCE;
        if (sort != null) {
            SortField[] sortFields = sort.getSort();
            List<SortField> list = new ArrayList<>(sortFields.length);
            for (SortField sortField : sortFields) {
                if (colName.equals(sortField.getField())) {
                    groupSort = new Sort(sortField);
                } else list.add(sortField);
            }
            if(!list.isEmpty()){
                SortField[] ins = new SortField[list.size()];
                withInGroup = new Sort(list.toArray(ins));
            }
        }
        groupingSearch.setGroupSort(groupSort);
        groupingSearch.setSortWithinGroup(withInGroup);
        groupingSearch.setFillSortFields(false);
        groupingSearch.setCaching(rowCount, false);
        groupingSearch.setAllGroups(true);
        groupingSearch.setAllGroupHeads(true);
        groupingSearch.setGroupDocsLimit(rowCount);
        groupingSearch.setGroupDocsOffset(start);
        return groupingSearch.search(indexSearcher, query, groupOffset, 1);
    }

}
