package index;

import bean.LSException;
import bean.Schema;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import org.apache.log4j.Level;
import util.Constants;
import util.Utils;

import java.util.Map;
import java.util.concurrent.Executors;

public class Indexer {

    //<indexName,indexImpl>,运行索引实例映射
    static final Cache<String, IndexImpl> indexes = CacheBuilder
            .newBuilder()
            .maximumSize(Constants.totalIndex)
            .removalListener(RemovalListeners.asynchronous(
                    (RemovalListener<String, IndexImpl>) notify -> {
                        if (notify.getCause() != RemovalCause.EXPLICIT) notify.getValue().close();
                        for (Map.Entry<String, String> entry : Searcher.mapper.entrySet()) {
                            if (entry.getValue().equals(notify.getKey())) {
                                String key = entry.getKey();
                                Searcher.mapper.remove(key);
                                Searcher.searches.invalidate(key);
                            }
                        }
                    },
                    Executors.newSingleThreadExecutor()))
            .build();

    public static boolean isRunning(String indexName) {
        return indexes.getIfPresent(indexName) != null;
    }

    public static void stopIndex(String indexName) {
        IndexImpl index = indexes.getIfPresent(indexName);
        if (index != null) index.close();
    }

    public static void index(String indexName) throws LSException {
        Schema schema = Utils.getSchema(indexName);
        IndexImpl impl = new IndexImpl(schema, Utils.getLogger(indexName, Level.DEBUG));
        Indexer.indexes.put(schema.getIndex(), impl);
        new Thread(impl).start();
    }

}
