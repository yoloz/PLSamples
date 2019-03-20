package api;

import org.apache.log4j.Logger;
import parser.CreateSql;
import util.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

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
            Utils.starApp(indexName);
        } catch (Exception e) {
            logger.error("create index[" + sql + "] error", e);
            error = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
        resp.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = resp.getOutputStream();
        if (error.isEmpty()) outputStream.write("{\"success\":true}".getBytes(StandardCharsets.UTF_8));
        else outputStream.write(error.getBytes(StandardCharsets.UTF_8));
    }
}
