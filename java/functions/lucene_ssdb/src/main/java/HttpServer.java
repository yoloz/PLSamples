import api.DelAllIndex;
import api.NewIndex;
import api.QueryIndex;
import api.StartIndex;
import api.StopIndex;
import bean.LSException;
import org.apache.log4j.PropertyConfigurator;
import util.Constants;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import util.Utils;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

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
        handler.addServletWithMapping(NewIndex.class, "/create");
        handler.addServletWithMapping(QueryIndex.class, "/query");
        handler.addServletWithMapping(StartIndex.class, "/start");
        handler.addServletWithMapping(StopIndex.class, "/stop");
        handler.addServletWithMapping(DelAllIndex.class, "/delAll");
        httpServer.start();
        httpServer.join();
    }

    public static void main(String[] args) {

        if (args == null || args.length < 1) {
            System.err.printf("command error...\n%s", "USAGE:*.sh start|stop");
            System.exit(1);
        }
        if (!"start".equals(args[0]) && !"stop".equals(args[0])) {
            System.err.printf("param %s is not defined\n%s", args[1], "USAGE:*.sh start|stop");
            System.exit(1);
        }
        PropertyConfigurator.configure(HttpServer.class.getResourceAsStream("/log4j.properties"));
        Logger logger = Logger.getLogger(HttpServer.class);
        try {
            if ("start".equals(args[0])) {
                Path pf = Constants.varDir.resolve("pid");
                if (!Files.notExists(pf, LinkOption.NOFOLLOW_LINKS)) throw new LSException("server pid is exit");
                HttpServer httpServer = new HttpServer();
                Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stopHttpServer));
                Files.write(Constants.varDir.resolve("pid"), ManagementFactory.getRuntimeMXBean()
                        .getName().split("@")[0].getBytes(StandardCharsets.UTF_8));
                httpServer.startHttpServer();
            } else {
                int exit = Utils.stopPid(Constants.varDir.resolve("pid"));
                logger.info("httpServer stop exit:[" + exit + "]");
            }
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            System.exit(1);
        }
        System.exit(0);
    }
}
