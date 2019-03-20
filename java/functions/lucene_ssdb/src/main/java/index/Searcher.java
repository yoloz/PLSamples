package index;

import bean.LSException;
import bean.Triple;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import index.parse.SelectSql;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import util.Constants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * lruCache<sql,page>:
 * lruCache size is Constants.searchCache
 * page size is Constants.pageCache
 */
public class Searcher {

    //<sqlId,<pullName,[show fields],[keys]>,([keys] size) means total <= Constants.pageCache*pageSize
    private final Cache<String, Triple<String, List<String>, List<Object>>> lru = CacheBuilder.newBuilder()
            .maximumSize(Constants.searchCache).build();

    private final String key;
    private final int offset;
    private final int limit;

    //"select xx" or [key,offset,limit]
    public Searcher(Object... params) {
        this.key = (String) params[0];
        this.offset = params[1] == null ? 1 : (int) params[1];
        this.limit = params[2] == null ? 0 : (int) params[2];
    }

    public Triple<String, List<String>, List<Object>> getKeys() throws LSException, JSQLParserException {
        String s = key.substring(0, 6).toLowerCase();
        if ("select".equals(s)) {
            SelectSql selectSql = new SelectSql(key);


        } else {
            Triple<String, List<String>, List<Object>> triple = lru.getIfPresent(key);
            if (triple == null) throw new LSException("缓存已经移除,请重新查询");
            List<Object> list = triple.getRight();
            //返回total<=pageCache*pageSize,下面错误理论不会出现
            if (list.size() < offset * limit) throw new LSException("请求大于缓存总数,请联系开发");
            List<Object> result = new ArrayList<>(limit);
            if (list.size() == offset * limit) for (int i = (offset - 1) * limit; i < offset * limit - 1; i++)
                result.add(list.get(i));
            else {
                if (list.size() < (offset + 1) * limit)
                    for (int i = offset * limit; i < list.size(); i++) result.add(list.get(i));
                else for (int i = offset * limit; i < (offset + 1) * limit; i++) result.add(list.get(i));
            }
            return Triple.of(triple.getLeft(), triple.getMiddle(), result);
        }
//        String key = this.md5(sql);


    }


    private String md5(String str) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return new String(Base64.getEncoder().encode(
                    md5.digest(str.getBytes(StandardCharsets.UTF_8))),
                    StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException ignored) {
        }
        return str;
    }

    class SearchImpl {

        private final IndexSearcher searcher;
        private final Query query;

        public SearchImpl(IndexSearcher searcher, Query query) {
            this.searcher = searcher;
            this.query = query;
        }


    }
}
