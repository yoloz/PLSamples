package api;

import index.Indexer;
import org.apache.log4j.Logger;
import util.JsonUtil;
import util.SqlliteUtil;
import util.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAllIndex extends HttpServlet {

    private final Logger logger = Logger.getLogger(GetAllIndex.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("get all index");
        String error = "";
        Map<String, Object> results = new HashMap<>();
        try {
            results.put("success", true);
            List<Object> list = SqlliteUtil.queryL("select name from schema");
            results.put("indexes", list);
            List<String> running = new ArrayList<>(Indexer.indexes.asMap().keySet());
            results.put("running", running);
        } catch (Exception e) {
            logger.error("get all index error,", e);
            error = Utils.responseError(e.getMessage());
        }
        resp.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = resp.getOutputStream();
        if (error.isEmpty())
            outputStream.write(JsonUtil.toString(results).getBytes(StandardCharsets.UTF_8));
        else outputStream.write(error.getBytes(StandardCharsets.UTF_8));
    }
}
