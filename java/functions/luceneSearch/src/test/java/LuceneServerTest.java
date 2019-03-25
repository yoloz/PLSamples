import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

public class LuceneServerTest {

    @Before
    public void setUp() {
    }


    /**
     * 测试前需要创建文件${LSDir}/conf/server.properties,否则Constants解析失败直接退出
     */
    @Test
    public void startHttpServer() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        System.setProperty("LSDir",
                Paths.get(LuceneServer.class.getResource("/schema_template.yaml").getPath())
                        .getParent().toString());
        System.setProperty("LogLevel","debug");
        System.out.println(LuceneServer.class.getResource("/log4j.properties").getPath());
        PropertyConfigurator.configure(LuceneServer.class.getResource("/log4j.properties").getPath());
        LuceneServer luceneServer = new LuceneServer();
        Method startHttpServer = luceneServer.getClass().getDeclaredMethod("startHttpServer");
        startHttpServer.setAccessible(true);
        startHttpServer.invoke(luceneServer);
    }

    @After
    public void tearDown() {
    }
}