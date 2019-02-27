import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

public class HttpServerTest {

    @Before
    public void setUp() throws Exception {
    }


    /**
     * 测试前需要创建文件${LSDir}/conf/server.properties,否则Constants解析失败直接退出
     */
    @Test
    public void startHttpServer() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        System.setProperty("LSDir",
                Paths.get(HttpServer.class.getResource("/schema_template.yaml").getPath())
                        .getParent().toString());
        PropertyConfigurator.configure(HttpServer.class.getResource("/log4j.properties").getPath());
        HttpServer httpServer = new HttpServer();
        Method startHttpServer = httpServer.getClass().getDeclaredMethod("startHttpServer");
        startHttpServer.setAccessible(true);
        startHttpServer.invoke(httpServer);
    }

    @After
    public void tearDown() throws Exception {
    }
}