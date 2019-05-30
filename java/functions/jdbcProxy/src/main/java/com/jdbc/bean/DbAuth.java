package com.jdbc.bean;

import com.JPServer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DbAuth {

    //todo guava load cache from database
    //key:dbkey+user
    private static Map<String, List<String>> userAuth;
    //key:dbkey+user+db
    private static Map<String, List<String>> dbAuth;
    //key:dbkey+user+db+tb
    private static Map<String, Map<String, List<String>>> tbAuth;
    //key:dbkey+user+db+tb+cn
    private static Map<String, List<String>> colAuth;


    /**
     * 对sql中的库,表,列鉴权是否允许执行
     * 列的查询请求这里不做处理,在返回结果集的时候处理
     */
    public static void authSql(String uk, String dbName, SqlInfo... sqlInfos)
            throws SQLException {
        if (sqlInfos == null || sqlInfos.length == 0) return;
        String dk = uk + dbName;
        for (SqlInfo sqlInfo : sqlInfos) {
            String tbName = sqlInfo.getName();
            Set<String> operators = sqlInfo.getOperators();
            authTable(operators, uk, dk, tbName);
            Map<SqlInfo.PairName, Set<String>> cols = sqlInfo.getCols();
            for (SqlInfo.PairName pairName : cols.keySet()) {
                authColumn(cols.get(pairName), uk, dk, tbName, pairName.getName());
            }
        }
    }

    static boolean selectCol(String uKey, String dKey, String tbName, String colName) {
        String operator = "select";
        String tbKey = dKey + tbName;
        String colKey = tbKey + colName;
        if (!userAuth.get(uKey).contains(operator)) {
            if (!dbAuth.get(dKey).contains(operator)) {
                List<String> colList = tbAuth.get(tbKey).get("col");
                if (!colList.contains(operator)) {
                    return colAuth.get(colKey).contains(operator);
                }

            }
        }
        return true;
    }


    private static void authTable(Set<String> operators, String uKey, String dKey, String tbName)
            throws SQLException {
        String tbKey = dKey + tbName;
        for (String operator : operators) {
            if (!userAuth.get(uKey).contains(operator)) {
                if (!dbAuth.get(dKey).contains(operator)) {
                    List<String> tbList = tbAuth.get(tbKey).get("tb");
                    if (!tbList.contains(operator))
                        throw new SQLException("[" + operator + "]table[" + tbName + "] permission denied");
                }
            }
        }
    }

    private static void authColumn(Set<String> operators, String uKey, String dbKey, String tbName,
                                   String colName) throws SQLException {
        String tbKey = dbKey + tbName;
        String colKey = tbKey + colName;
        for (String operator : operators) {
            if (!"select".equals(operator)) {
                if (!userAuth.get(uKey).contains(operator)) {
                    if (!dbAuth.get(dbKey).contains(operator)) {
                        List<String> colList = tbAuth.get(tbKey).get("col");
                        if (!colList.contains(operator)) {
                            if (!colAuth.get(colKey).contains(operator))
                                throw new SQLException("[" + operator + "]column[" +
                                        tbName + "." + colName + "] permission denied");
                        }

                    }
                }
            }
        }
    }

    static {
        try (InputStream in = Files.newInputStream(Paths.get(JPServer.JPPath, "conf", "sqlAuth.yaml"))) {
            Iterator<Object> iterator = new Yaml().loadAll(in).iterator();
            int index = 0;
            while (iterator.hasNext()) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) iterator.next();
                for (Map<String, Object> map : list) {
                    String key;
                    List<String> privs;
                    switch (index) {
                        case 0://user
                            if (userAuth == null) userAuth = new HashMap<>();
                            key = String.valueOf(map.get("dbkey")) + map.get("user");
                            privs = (List<String>) map.get("priv");
                            userAuth.put(key, privs);
                            break;
                        case 1://db
                            if (dbAuth == null) dbAuth = new HashMap<>();
                            key = String.valueOf(map.get("dbkey")) + map.get("user")
                                    + map.get("db");
                            privs = (List<String>) map.get("priv");
                            dbAuth.put(key, privs);
                            break;
                        case 2://table
                            if (tbAuth == null) tbAuth = new HashMap<>();
                            key = String.valueOf(map.get("dbkey")) + map.get("user")
                                    + map.get("db") + map.get("tb");
                            privs = (List<String>) map.get("tb_priv");
                            List<String> col_privs = (List<String>) map.get("col_priv");
                            Map<String, List<String>> m = new HashMap<>(2);
                            m.put("tb", privs);
                            m.put("col", col_privs);
                            tbAuth.put(key, m);
                            break;
                        case 3://column
                            if (colAuth == null) colAuth = new HashMap<>();
                            key = String.valueOf(map.get("dbkey")) + map.get("user")
                                    + map.get("db") + map.get("tb") + map.get("cn");
                            privs = (List<String>) map.get("priv");
                            colAuth.put(key, privs);
                            break;
                    }
                }
                index += 1;
            }
        } catch (IOException e) {
            System.err.println("init load authority error," + e);
            System.exit(1);
        }
    }
}
