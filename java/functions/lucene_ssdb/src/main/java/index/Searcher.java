package index;

import bean.LSException;
import bean.Pair;
import bean.Source;
import bean.Triple;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import index.parse.SelectSql;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.log4j.Logger;
import util.Constants;
import util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * lruCache<sql,page>:
 * lruCache size is Constants.searchCache
 * page size is Constants.pageCache
 */
public class Searcher {

    private final Logger logger = Logger.getLogger(Searcher.class);

    //<sqlId,<[show fields],[<pullName,key>],total>>,total<=Constants.pageCache*pageSize
    static final Cache<String, Triple<List<String>, List<Pair<String, Object>>, Integer>> searches = CacheBuilder
            .newBuilder()
            .maximumSize(Constants.searchCache)
            .build();

    private String key;
    private int start;
    private int rowCount;
    private Source source;

    //"select xx" or [key,offset,limit]
    public Searcher(Object... params) {
        this.key = (String) params[0];
        this.rowCount = params[2] == null ? 0 : (int) params[2];
        this.start = params[1] == null ? 0 : ((int) params[1]) * rowCount;
    }

    //{"total":,"list":[<pullName,key>...],"cols":[f1,f2...]}
    public Map<String, Object> getKeys() throws LSException, JSQLParserException, IOException {
        String s = key.substring(0, 6).toLowerCase();
        if ("select".equals(s)) {
            String sql = key;
            SelectSql selectSql = new SelectSql(sql);
            List<String> cols = selectSql.getSelects();
            Pair<Integer, Integer> _limit = selectSql.getLimit();
            start = _limit.getLeft();
            rowCount = _limit.getRight();
            String indexName = selectSql.getIndexName();
            SearchImpl searchImpl = new SearchImpl(selectSql);
            IndexImpl indexImpl = Indexer.indexes.getIfPresent(indexName);
            if (indexImpl == null) {
                logger.info("索引[" + indexName + "]非运行中,IndexReader查询");
                return searchImpl.offSearch();
            } else {
                logger.info("索引[" + indexName + "]运行中,近实时查询");
                key = Utils.md5(sql);
                if (searches.getIfPresent(key) != null) {
                    logger.warn("清空索引[" + indexName + "]查询[" + sql + "]缓存,重新查询");
                    searches.invalidate(key);
                }
                searchImpl.nrtSearch(key, indexImpl.getSearcher());
            }
        }
        logger.info("取缓存[" + key + "]分页数据");
        Triple<List<String>, List<Pair<String, Object>>, Integer> triple = searches.getIfPresent(key);
        if (triple == null) throw new LSException("缓存已经移除,请重新查询");
        List<Pair<String, Object>> cache = triple.getMiddle();
        //返回total<=pageCache*pageSize,下面错误理论不会出现
        if (cache.size() < start) throw new LSException("请求数据越界[start > cacheSize]");
        List<Object> list = new ArrayList<>(rowCount);
        int end = Math.min(cache.size(), start + rowCount);
        for (int i = start; i < end; i++) list.add(cache.get(i));
        Map<String, Object> map = new HashMap<>(3);
        map.put("total", triple.getRight());
        map.put("list", list);
        map.put("cols", triple.getLeft());
        return map;
    }

}
