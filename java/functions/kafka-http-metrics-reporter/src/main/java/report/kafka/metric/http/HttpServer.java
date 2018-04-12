package report.kafka.metric.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private Server server;
    private int port;
    private String bindAddress;

    /**
     * Method: HttpServer
     * Purpose: Method for constructing the metrics server.
     *
     * @param bindAddress the name or address to bind on ( defaults to localhost )
     * @param port            the port to bind on ( defaults to 8080 )
     */
    HttpServer(final String bindAddress, final int port) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.init();
    }

    /**
     * Method: init
     * Purpose: Initializes the embedded Jetty Server with including the metrics servlets.
     */
    private void init() {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(bindAddress, port);
        server = new Server(inetSocketAddress);
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(MetricsServlet.class, "/api/metrics");
        server.setHandler(servletHandler);
    }

    /**
     * Method: start
     * Purpose: starting the metrics server
     */
    void start() {
        try {
            logger.info("Starting Kafka Http Metrics Reporter");
            // starting the Jetty Server
            server.start();
            logger.info("Started Kafka Http Metrics Reporter on: " + bindAddress + ":" + port);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Method: stop
     * Purpose: Stopping the metrics server
     */
    void stop() {
        try {
            logger.info("Stopping Kafka Http Metrics Reporter");
            // stopping the Jetty Server
            server.stop();
            logger.info("Kafka Http Metrics Reporter stopped");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}

