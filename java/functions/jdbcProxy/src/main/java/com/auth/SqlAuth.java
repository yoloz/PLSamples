package com.auth;

import com.JPServer;
import com.parse.SQLParse;
import com.source.Connect;
import net.sf.jsqlparser.JSQLParserException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SqlAuth {

    private Connection connect;

    private String db;
    private String user;
    private String sql;


    public SqlAuth(Connect connect, String sql) {
        this.connect = connect.getConnection();
        this.db = connect.getDb();
        this.user = connect.getUser();
        this.sql = sql;
    }

    @SuppressWarnings("unchecked")
    public boolean check() throws JSQLParserException {
        SQLParse sqlParse = new SQLParse(connect);
        Map<String, Map<String, Object>> operations = sqlParse.getOperation(sql);
        Map<String, Map<String, List<String>>> auths = getPrivilege(operations.keySet());
        for (String tb : operations.keySet()) {
            if (!auths.containsKey(tb)) return false;
            Map<String, List<String>> am = auths.get(tb);
            Map<String, Object> opm = operations.get(tb);
            for (String key : opm.keySet()) {
                List<String> privs = am.get(key);
                if (privs == null) return false;
                Object v = opm.get(key);
                if (v instanceof String) {
                    String ops = (String) v;
                    if (!privs.contains(ops)) return false;
                } else {
                    Set<String> ops = (Set<String>) v;
                    for (String op : ops) if (!privs.contains(op)) return false;
                }
            }
        }
        return true;
    }

    //todo 临时使用方式
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, List<String>>> getPrivilege(Set<String> tables) {
        Map<String, Map<String, List<String>>> map = new HashMap<>(tables.size());
        try (InputStream in = Files.newInputStream(Paths.get(JPServer.JPPath, "conf", "authority.yaml"))) {
            List<Map<String, Object>> list = new Yaml().loadAs(in, ArrayList.class);
            list.forEach(obj -> {
                String tb = (String) obj.get("tb");
                if (db.equals(obj.get("db")) && tables.contains(tb) && user.equals(obj.get("user"))) {
                    Map<String, List<String>> _m = new HashMap<>();
                    _m.put(tb, (List<String>) obj.get("table_priv"));
                    Map<String, List<String>> cols = (Map<String, List<String>>) obj.get("columns");
                    _m.putAll(cols);
                    map.put(tb, _m);
                }
            });

        } catch (IOException e) {
            System.err.println("init source mapping error," + e);
            System.exit(1);
        }
        return map;
    }
}
