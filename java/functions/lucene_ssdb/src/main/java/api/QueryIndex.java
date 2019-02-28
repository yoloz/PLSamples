package api;

import app.index.SearchIndex;
import bean.ImmutablePair;
import bean.ImmutableTriple;
import bean.Schema;
import org.apache.log4j.Logger;
import parser.QuerySql;
import util.JsonUtil;
import util.Utils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class QueryIndex extends HttpServlet {

    private final Logger logger = Logger.getLogger(QueryIndex.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sql = Utils.getInputStream(req.getInputStream());
        logger.debug("query sql=>[" + sql + "]");
        String error = "";
        Map<String, Object> results = null;
        try {
            QuerySql querySql = new QuerySql(sql);
            ImmutableTriple<ImmutablePair<List<String>, Integer>, String, Schema> triple = querySql.parse();
            SearchIndex searchIndex = new SearchIndex(triple.getRight());
            results = searchIndex.search(triple.getMiddle(), triple.getLeft().getLeft(), triple.getLeft().getRight());
            results.put("success", true);
        } catch (Exception e) {
            logger.error(e.getCause() == null ? e.getMessage() : e.getCause());
            error = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
        resp.setContentType("application/json;charset=UTF-8");
        OutputStream outputStream = resp.getOutputStream();
        if (error.isEmpty()) {
            outputStream.write(JsonUtil.toString(results).getBytes(StandardCharsets.UTF_8));
        } else outputStream.write(error.getBytes(StandardCharsets.UTF_8));
    }
}
