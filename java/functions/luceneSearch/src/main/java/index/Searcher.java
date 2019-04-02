package index;

import bean.LSException;
import bean.Pair;
import bean.Triple;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import util.Constants;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * lruCache<sql,page>:
 * lruCache size is Constants.searchCache*Constants.totalIndex
 * page size is Constants.pageCache
 */
public class Searcher {

    static final int max = Constants.searchCache * Constants.totalIndex;

    //sqlId<==>indexName
    static final Map<String, String> mapper = new ConcurrentHashMap<>(max);

    //<sqlId,<[show fields],[<pullName,key>],total>>,total<=Constants.pageCache*pageSize
    static final Cache<String, Triple<List<String>, List<Pair<String, Object>>, Integer>> searches = CacheBuilder
            .newBuilder()
            .maximumSize(max)
            .removalListener(RemovalListeners.asynchronous(
                    (RemovalListener<String, Triple<List<String>, List<Pair<String, Object>>, Integer>>)
                            notify -> mapper.remove(notify.getKey()),
                    Executors.newSingleThreadExecutor()))
            .build();

    public static Map<String, Object> search(Object... params) throws LSException, IOException {
        if (params == null) throw new LSException("搜索条件为空");
        SearchImpl searchImpl = new SearchImpl(params);
        return searchImpl.search();
    }
}
