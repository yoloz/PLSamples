package api;

import bean.LSException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StopIndex extends HttpServlet {

    private final Logger logger = Logger.getLogger(StopIndex.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String indexName = Utils.getInputStream(req.getInputStream());
        logger.debug("stop index=>[" + indexName + "]");
        String error = "";
        Map<String, Object> results = null;
        try {
            List<Map<String, Object>> list = SqlliteUtil
                    .query("select pid from ssdb where name=?", indexName);
            if (list.isEmpty()) throw new LSException("index[" + indexName + "] is not exit...");
            results = new HashMap<>(2);
            results.put("success", true);
            String pid = String.valueOf(list.get(0).get("pid"));
            if ("0".equals(pid)) {
                logger.warn("index[" + indexName + "] is not running");
                results.put("exit", pid);
            } else {
                int exit = Utils.stopPid(pid);
                Utils.updateAppStatus(String.valueOf(exit), indexName);
                results.put("exit", exit);
//                if (exit != 0) results.put("success", false);
            }
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            error = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
        resp.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = resp.getOutputStream();
        if (error.isEmpty())
            outputStream.write(JsonUtil.toString(results).getBytes(StandardCharsets.UTF_8));
        else outputStream.write(error.getBytes(StandardCharsets.UTF_8));
    }
}
