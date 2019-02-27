import api.AddIndex;
import api.NewIndex;
import api.QueryIndex;
import org.apache.log4j.PropertyConfigurator;
import util.Constants;
//import app.index.SearchIndex;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

//import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
//import java.nio.charset.Charset;
//import java.nio.file.Path;
//import java.nio.file.Paths;

public class HttpServer {

    private Server httpServer;

    private void stopHttpServer() {
        try {
            if (httpServer != null) httpServer.stop();
        } catch (Exception ignored) {
        }
    }

    private void startHttpServer() throws Exception {
        httpServer = new Server(Constants.httpPort);
        ServletHandler handler = new ServletHandler();
        httpServer.setHandler(handler);
        handler.addServletWithMapping(NewIndex.class, "/newIndex");
        handler.addServletWithMapping(AddIndex.class, "/addIndex");
        handler.addServletWithMapping(QueryIndex.class, "/queryIndex");
        httpServer.start();
        httpServer.join();
    }

    public static void main(String[] args) {

        if (args == null || args.length < 1) {
            System.err.printf("command error...\n%s", "USAGE:*.sh start|stop");
            System.exit(-1);
        }
        if (!"start".equals(args[0]) && !"stop".equals(args[0])) {
            System.err.printf("param %s is not defined\n%s", args[1], "USAGE:*.sh start|stop");
            System.exit(-1);
        }
        PropertyConfigurator.configure(HttpServer.class.getResourceAsStream("/log4j.properties"));
        Logger logger = Logger.getLogger(HttpServer.class);
        try {
            if ("start".equals(args[0])) {
                HttpServer httpServer = new HttpServer();
                Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stopHttpServer));
                httpServer.startHttpServer();
            }
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            System.exit(-1);
        }
        System.exit(0);
    }

//    private void testOneSearch(Path indexPath, String query, int expectedHitCount) throws Exception {
//        PrintStream outSave = System.out;
//        String output = "";
//        try {
//            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//            PrintStream fakeSystemOut = new PrintStream(bytes, false, Charset.defaultCharset().name());
//            System.setOut(fakeSystemOut);
//            SearchIndex.main(new String[]{"-query", query, "-index", indexPath.toString()});
//            fakeSystemOut.flush();
//            output = bytes.toString(Charset.defaultCharset().name()); // intentionally use default encoding
////      assertTrue("output=" + output, output.contains(expectedHitCount + " total matching documents"));
//        } finally {
//            System.setOut(outSave);
//        }
//        System.out.println(output);
//    }
//
//    public void testIndexSearch() throws Exception {
//        Path dir = getDataPath("test-files/docs");
////    Path indexDir = createTempDir("ContribDemoTest");
//        Path indexDir = Paths.get("/home/ylzhang/projects/lucene-solr/lucene/demo/src/test/org/apache/lucene/demo/ContribDemoTest");
////    WriteIndex.main(new String[] { "-create", "-docs", dir.toString(), "-index", indexDir.toString()});
//        testOneSearch(indexDir, "apache", 3);
//        testOneSearch(indexDir, "patent", 8);
//        testOneSearch(indexDir, "lucene", 0);
//        testOneSearch(indexDir, "gnu", 6);
//        testOneSearch(indexDir, "derivative", 8);
//        testOneSearch(indexDir, "license", 13);
//    }
//
//    private Path getDataPath(String s) {
//        return null;
//    }
}
