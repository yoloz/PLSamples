package api;

import util.Constants;
import bean.LSException;
import org.apache.log4j.Logger;
import parser.CreateSql;
import util.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class NewIndex extends HttpServlet {

    private Logger logger = Logger.getLogger(NewIndex.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sql = Utils.getInputStream(req.getInputStream());
        logger.debug("create sql=>[" + sql + "]");
        String error = "";
        try {
            CreateSql createSql = new CreateSql(sql);
            String indexName = createSql.parse();
            ProcessBuilder process = new ProcessBuilder();
            List<String> commands = Utils.getCommand(indexName);
            commands.add(indexName);
            commands.add("create_append");
            process.command(commands);
            process.redirectErrorStream(true);
            process.redirectOutput(Constants.logDir.resolve(indexName + ".out").toFile());
            process.start();
        } catch (Exception e) {
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
