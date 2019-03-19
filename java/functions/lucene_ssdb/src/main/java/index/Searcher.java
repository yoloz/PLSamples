package index;

import bean.ImmutablePair;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import util.Constants;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

/**
 * lruCache<sql,page>:
 * lruCache size is Constants.searchCache
 * page size is Constants.pageCache
 */
public class Searcher {

    private final Cache<String, List<String>> lru = CacheBuilder.newBuilder()
            .maximumSize(Constants.searchCache).build();

    private final String sql;

    public Searcher(String sql) {
        this.sql = sql;
    }

    public List<String> getKeys(String sql) {
        String key = this.md5(sql);
        if (lru.getIfPresent(key) != null) {

        }

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
