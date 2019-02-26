package api;

import bean.LSException;
import org.apache.log4j.Logger;
import util.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CreateIndex extends HttpServlet {

    private Logger logger = Logger.getLogger(CreateIndex.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sql = Utils.getInputStream(req.getInputStream());
        logger.debug("createIndex sql=>" + sql);
        resp.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = resp.getOutputStream();
        crud.CreateIndex createIndex = new crud.CreateIndex(sql);
        String error = "";
        try {
            createIndex.create();
        } catch (LSException e) {
            logger.error(e);
            error = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
        if (error.isEmpty())
            outputStream.write("{\"success\":true}".getBytes(StandardCharsets.UTF_8));
        else outputStream.write(error.getBytes(StandardCharsets.UTF_8));
    }
}
