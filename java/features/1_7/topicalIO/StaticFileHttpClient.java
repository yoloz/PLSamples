package topicalIO;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 *  on 16-8-21.
 * 基于文件系统中静态文件的HTTP测试客户端
 */
public class StaticFileHttpClient {

    public static void main(String[] args) {
        LoadWebPageUseSelector selector = new LoadWebPageUseSelector();
        try {
            Set<URL> urls = new HashSet<>(3);
//            urls.add(new URL("http://127.0.0.1:10080/test1.txt"));
            urls.add(new URL("http://localhost:10080/test2.json"));
            selector.load(urls);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
