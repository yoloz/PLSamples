package api;

import bean.LSException;
import index.Indexer;
import org.apache.log4j.Logger;
import util.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class StopIndex extends HttpServlet {

    private final Logger logger = Logger.getLogger(StopIndex.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String indexName = Utils.getInputStream(req.getInputStream());
        logger.debug("stop index=>[" + indexName + "]");
        String error = "";
        try {
            if (!Indexer.isRunning(indexName))
                throw new LSException("index[" + indexName + "] is not running");
            Indexer.stopIndex(indexName);
        } catch (Exception e) {
            logger.error("stop index[" + indexName + "] error", e);
            error = Utils.responseError(e.getMessage());
        }
        resp.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = resp.getOutputStream();
        if (error.isEmpty())
            outputStream.write("{\"success\":true}".getBytes(StandardCharsets.UTF_8));
        else outputStream.write(error.getBytes(StandardCharsets.UTF_8));
    }
}
