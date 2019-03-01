import api.AddIndex;
import api.NewIndex;
import api.QueryIndex;
import org.apache.log4j.PropertyConfigurator;
import util.Constants;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import util.Utils;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
                HttpServer httpServer = new HttpServer();
                Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stopHttpServer));
                httpServer.startHttpServer();
                Files.createDirectory(Constants.varDir);
                Files.write(Constants.varDir.resolve("pid"), ManagementFactory.getRuntimeMXBean()
                        .getName().split("@")[0].getBytes(StandardCharsets.UTF_8));
            } else Utils.stopPid(Constants.varDir.resolve("pid"));
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            System.exit(1);
        }
        System.exit(0);
    }
}
