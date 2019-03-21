package index;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalListeners;
import util.Constants;

import java.util.concurrent.Executors;

public class Indexer {

    //<indexName,indexImpl>,运行索引实例映射
     static final Cache<String, IndexImpl> indexes = CacheBuilder
            .newBuilder()
            .maximumSize(Constants.totalIndex)
            .removalListener(RemovalListeners.asynchronous(
                    (RemovalListener<String, IndexImpl>) notify -> notify.getValue().close(),
                    Executors.newSingleThreadExecutor()))
            .build();
}
