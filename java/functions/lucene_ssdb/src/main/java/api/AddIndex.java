package api;

import util.Constants;
import org.apache.log4j.Logger;
import util.SqlliteUtil;
import util.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AddIndex extends HttpServlet {

    private Logger logger = Logger.getLogger(AddIndex.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String indexName = Utils.getInputStream(req.getInputStream());
        logger.debug("add index=>[" + indexName + "]");
        String error = "";
        try {
            List<Map<String, Object>> list = SqlliteUtil
                    .query("select value from schema where name=?", indexName);
            if (list.isEmpty()) throw new IOException("index[" + indexName + "] is not exit...");
            ProcessBuilder process = new ProcessBuilder();
            List<String> commands = Utils.getCommand(indexName);
            commands.add(indexName);
            commands.add("create_append");
            process.command(commands);
            process.redirectErrorStream(true);
            process.redirectOutput(Constants.logDir.resolve(indexName + ".out").toFile());
            process.start();
        } catch (SQLException | IOException e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            error = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
        resp.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = resp.getOutputStream();
        if (error.isEmpty())
            outputStream.write("{\"success\":true}".getBytes(StandardCharsets.UTF_8));
        else outputStream.write(error.getBytes(StandardCharsets.UTF_8));
    }
}
