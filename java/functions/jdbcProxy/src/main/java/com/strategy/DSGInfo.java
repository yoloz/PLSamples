package com.strategy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.handler.IOHandler;
import com.handler.PermissionException;
import com.jdbc.bean.SqlInfo;
import com.util.Constants;
import com.util.InnerDb;
import org.apache.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;


public class DSGInfo {

    private final static Logger logger = Logger.getLogger(DSGInfo.class);

    private static Map<String, List<Map<String, Object>>> drivers;

    static {
        try (Reader reader = Files.newBufferedReader(Paths.get(Constants.JPPath, "conf",
                "proxyDriver.json"), StandardCharsets.UTF_8)) {
            drivers = new Gson().fromJson(reader, new TypeToken<Map<String, List<Map<String, Object>>>>() {
            }.getType());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException("load proxy driver error=>", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getAppInfo(String ak) throws Exception {
        String sql = "select id,client,mac,process,server_ip,server_port,server_type,server_version," +
                "username,password,default_database,dbms_id,properties " +
                "from data_cap_use_certification where ak=?";
        Map<String, Object> map = InnerDb.get(sql, ak);
        String server_type = String.valueOf(map.get("server_type")).toLowerCase();
        String password = String.valueOf(map.remove("password"));
        password = password.substring(2, password.length() - 1).toUpperCase();
        String k = "Encrypt@12345678";
        byte[] pbytes = IOHandler.hexToByte(password);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keyspec = new SecretKeySpec(k.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivspec = new IvParameterSpec(k.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
        byte[] original = cipher.doFinal(pbytes);
        password = new String(original, StandardCharsets.UTF_8).trim();
        map.put("password", password);
        String server_version = String.valueOf(map.remove("server_version"));
        if (drivers.containsKey(server_type)) {
            List<Map<String, Object>> list = drivers.get(server_type);
            for (Map<String, Object> objectMap : list) {
                List<String> versions = (List<String>) objectMap.get("version");
                if (versions.contains(server_version)) {
                    map.put("driverClass", objectMap.get("driverClass"));
                    map.put("driverPath", objectMap.get("driverPath"));
                    return map;
                }
            }
            throw new SQLException(server_type + " version[" + server_version + "] driver undefined");
        } else throw new SQLException("DB[" + server_type + "," + server_version + "] driver undefined");
    }

    /**
     * @param app_id      appid
     * @param platform_id platformid
     * @param user        username
     * @param sqlInfos    sql
     * @return no select permission cols
     * @throws SQLException e
     */
    public static Set<String> checkPermission(int app_id, int platform_id, String user, List<SqlInfo> sqlInfos)
            throws SQLException, PermissionException {
        Set<String> operators = new HashSet<>();
        for (SqlInfo info : sqlInfos) {
//            operators.addAll(info.getOperators());
            for (Set<String> set : info.getCols().values()) {
                operators.addAll(set);
            }
        }
        if (operators.isEmpty()) return Collections.emptySet();
        List<Map<String, Object>> list;
        if (!user.isEmpty()) {
            StringBuilder sqlBuilder = new StringBuilder("select action_name,data_database,data_table,data_col");
            sqlBuilder.append(" from data_model where data_type=? and")
                    .append(" data_id=(select platform_unified_id from data_platform_user where")
                    .append(" platform_id=? and username=?) and action_name in(");
            for (String operator : operators) {
                sqlBuilder.append("'").append(operator.toUpperCase()).append("'").append(",");
            }
            String sql = sqlBuilder.toString();
            sql = sql.substring(0, sql.length() - 1) + ")";
            list = InnerDb.query(sql, 1, platform_id, user);
        } else {
            StringBuilder sqlBuilder = new StringBuilder("select action_name,data_database,data_table,data_col");
            sqlBuilder.append(" from data_model where ").append("data_type=? and data_id=? and action_name in(");
            for (String operator : operators) {
                sqlBuilder.append("'").append(operator.toUpperCase()).append("'").append(",");
            }
            String sql = sqlBuilder.toString();
            sql = sql.substring(0, sql.length() - 1) + ")";
            list = InnerDb.query(sql, 2, app_id);
        }
        String flag = user.isEmpty() ? app_id + "" : user;
        if (list.isEmpty()) throw new PermissionException(flag + " data_model is empty");
        Map<String, Map<String, List<String>>> dtc = new HashMap<>(list.size());
        for (Map<String, Object> map : list) {
            String operator = String.valueOf(map.get("action_name")).toLowerCase();
            Map<String, List<String>> om = new HashMap<>(3);
            String database = String.valueOf(map.get("data_database"));
            if (!database.equals("null") && !database.isEmpty()) {
                database = database.substring(1, database.length() - 1);
                List<String> db = new ArrayList<>();
                Collections.addAll(db, database.split("&"));
                om.put("db", db);
            }
            String table = String.valueOf(map.get("data_table"));
            if (!table.equals("null") && !table.isEmpty()) {
                table = table.substring(1, table.length() - 1);
                List<String> tb = new ArrayList<>();
                Collections.addAll(tb, table.split("&"));
                om.put("tb", tb);
            }
            String cols = String.valueOf(map.get("data_col"));
            if (!cols.equals("null") && !cols.isEmpty()) {
                cols = cols.substring(1, cols.length() - 1);
                List<String> col = new ArrayList<>();
                Collections.addAll(col, cols.split("&"));
                om.put("col", col);
            }
            dtc.put(operator, om);
        }
        return checkSQL(flag, sqlInfos, dtc);
    }

    /**
     * 在resultSet返回结果中当noSelectCols为null时,需要自己查询是否可select
     *
     * @param app_id      appid
     * @param platform_id platformId
     * @param user        username
     * @return select permission
     * @throws SQLException e
     */
    public static Map<String, List<String>> getSelectPermission(int app_id, int platform_id, String user)
            throws SQLException {
        List<Map<String, Object>> list;
        if (!user.isEmpty()) {
            String sql = "select data_database,data_table,data_col" +
                    " from data_model where data_type=? and" +
                    " data_id=(select platform_unified_id from data_platform_user where" +
                    " platform_id=? and username=?) and action_name=?";
            list = InnerDb.query(sql, 1, platform_id, user, "SELECT");
        } else {
            String sql = "select data_database,data_table,data_col" +
                    " from data_model where " + "data_type=? and data_id=? and action_name=?";
            list = InnerDb.query(sql, 2, app_id, "SELECT");
        }
        if (list.isEmpty()) return null;
        Map<String, List<String>> om = new HashMap<>(3);
        String database = String.valueOf(list.get(0).get("data_database"));
        if (!database.equals("null") && !database.isEmpty()) {
            database = database.substring(1, database.length() - 1);
            List<String> db = new ArrayList<>();
            Collections.addAll(db, database.split("&"));
            om.put("db", db);
        }
        String table = String.valueOf(list.get(0).get("data_table"));
        if (!table.equals("null") && !table.isEmpty()) {
            table = table.substring(1, table.length() - 1);
            List<String> tb = new ArrayList<>();
            Collections.addAll(tb, table.split("&"));
            om.put("tb", tb);
        }
        String cols = String.valueOf(list.get(0).get("data_col"));
        if (!cols.equals("null") && !cols.isEmpty()) {
            cols = cols.substring(1, cols.length() - 1);
            List<String> col = new ArrayList<>();
            Collections.addAll(col, cols.split("&"));
            om.put("col", col);
        }
        return om;
    }

    /**
     * 对sql中的库,表,列鉴权是否允许执行
     * 列的查询请求这里不做处理,在返回结果集的时候处理
     * flag: default userName,else connection AK
     *
     * @param sqlInfos 拆解后的库表列信息
     * @param dtc      静态安全策略<operator,<db|tb|col,[original]>>
     */
    private static Set<String> checkSQL(String flag, List<SqlInfo> sqlInfos, Map<String, Map<String, List<String>>> dtc)
            throws PermissionException {
        Set<String> noSC = new HashSet<>();
        for (SqlInfo sqlInfo : sqlInfos) {
            String dbName = sqlInfo.getDbName();
            Map<SqlInfo.PairName, Set<String>> cols = sqlInfo.getCols();
            for (SqlInfo.PairName pc : cols.keySet()) {
                String cn = sqlInfo.toString() + "." + pc.getName();
                for (String operator : cols.get(pc)) {
                    Map<String, List<String>> map = dtc.get(operator);
                    if (map == null)
                        throw new PermissionException(flag + " has no permission to[" + operator + "]" + dbName);
                    List<String> col = map.get("col");
                    List<String> db = map.get("db");
                    List<String> tb = map.get("tb");
                    if (db == null || !db.contains(dbName)) {
                        if (tb == null || !tb.contains(sqlInfo.toString())) {
                            if (col == null || !col.contains(cn)) {
                                if ("select".equals(operator)) noSC.add(cn);
                                else throw new PermissionException(flag + " has no permission to["
                                        + operator + "]" + cn);
                            }
                        }
                    }
                }
            }
        }
        return noSC;
    }

    public static Map<String, String> filterRow(String ak, String user, String table) throws SQLException {
        String sql = "select colname,filterval from data_rowfilter_temp where ak=? and username=? and tablename=?";
        List<Map<String, Object>> list = InnerDb.query(sql, ak, user, table);
        Map<String, String> map = new HashMap<>(list.size());
        for (Map<String, Object> objectMap : list) {
            map.put(String.valueOf(objectMap.get("colname")), String.valueOf(objectMap.get("filterval")));
        }
        return map;
    }
}
