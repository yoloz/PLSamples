package com.auth;

import com.JPServer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Authority {

    private String db;
    private String tb;
    private String user;

    private Map<String, List<String>> privileges = new HashMap<>();

    public Authority(String db, String tb, String user) {
        this.db = db;
        this.tb = tb;
        this.user = user;
        this.getPriv();
    }

    //todo 临时使用方式
    @SuppressWarnings("unchecked")
    private void getPriv() {
        try (InputStream in = Files.newInputStream(Paths.get(JPServer.JPPath, "conf", "authority.yaml"))) {
            List<Map<String, Object>> list = new Yaml().loadAs(in, ArrayList.class);
            list.forEach(obj -> {
                if (db.equals(obj.get("db")) && tb.equals(obj.get("tb")) && user.equals(obj.get("user"))) {
                    privileges.put(tb, (List<String>) obj.get("table_priv"));
                    Map<String, List<String>> cols = (Map<String, List<String>>) obj.get("columns");
                    privileges.putAll(cols);
                }
            });
        } catch (IOException e) {
            System.err.println("init source mapping error," + e);
            System.exit(1);
        }
    }

    public List<String> getPrivilege(String key) {
        return privileges.get(key);
    }
}
