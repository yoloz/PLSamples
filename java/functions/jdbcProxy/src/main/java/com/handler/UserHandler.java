package com.handler;

import com.jdbc.bean.SqlInfo;
import com.jdbc.bean.WrapConnect;
import com.jdbc.sql.parser.SQLParserUtils;
import com.jdbc.sql.parser.SQLStatementParser;
import com.util.InnerDb;

import java.sql.SQLException;
import java.util.*;

public class UserHandler {

//    public static Object login(String user, String pwd) throws SQLException {
//        String sql = "select pwd,priv from proxyuser where name=?";
//        Map<String, Object> map = InnerDb.get(sql, user);
//        if (map.isEmpty()) throw new SQLException("用户[" + user + "] 不存在");
//        String _pwd = String.valueOf(map.get("pwd"));
//        if (!_pwd.equals(pwd)) throw new SQLException("用户[" + user + "] 密码不正确");
//        return map.get("priv");
//    }

    public static List<String> splitComma(Object obj) {
        List<String> list = new ArrayList<>();
        if (obj != null) {
            String[] parr = String.valueOf(obj).split(",");
            Collections.addAll(list, parr);
        }
        return list;
    }

    public static Object userAuth(String user) throws SQLException {
        String sql = "select priv from proxyuser where name=?";
        Map<String, Object> map = InnerDb.get(sql, user);
        return map.get("priv");
    }

    public static List<String> dbAuth(String dbkey, String user) throws SQLException {
        String sql = "select priv from proxydbauth where dbkey=? and username=?";
        Map<String, Object> map = InnerDb.get(sql, dbkey, user);
        return splitComma(map.get("priv"));
    }

    public static Map<String, List<String>> tbAuth(String dbkey, String user, String table) throws SQLException {
        String sql = "select priv,colpriv from proxytableauth where dbkey=? and username=? and tablename=?";
        Map<String, Object> map = InnerDb.get(sql, dbkey, user, table);
        Map<String, List<String>> m = new HashMap<>(2);
        m.put("priv", splitComma(map.get("priv")));
        m.put("colpriv", splitComma(map.get("colpriv")));
        return m;
    }

    public static Map<String, List<String>> colAuth(String dbkey, String user, String table) throws SQLException {
        String sql = "select colname,priv from proxycolauth where dbkey=? and username=? and tablename=?";
        List<Map<String, Object>> list = InnerDb.query(sql, dbkey, user, table);
        Map<String, List<String>> map = new HashMap<>(list.size());
        for (Map<String, Object> objectMap : list) {
            map.put(String.valueOf(objectMap.get("colname")), splitComma(objectMap.get("priv")));
        }
        return map;
    }

    public static Map<String, String> filterRow(String dbkey, String user, String table) throws SQLException {
        String sql = "select colname,filterval from proxyfilter where dbkey=? and username=? and tablename=?";
        List<Map<String, Object>> list = InnerDb.query(sql, dbkey, user, table);
        Map<String, String> map = new HashMap<>(list.size());
        for (Map<String, Object> objectMap : list) {
            map.put(String.valueOf(objectMap.get("colname")), String.valueOf(objectMap.get("filterval")));
        }
        return map;
    }

    public static void authSql(String user, String dbkey, List<SqlInfo> sqlInfoList)
            throws SQLException, PermissionException {
        Object userPriv = userAuth(user);
        for (SqlInfo sqlInfo : sqlInfoList) {
            authSql(userPriv, dbkey, user, sqlInfo);
        }
    }

    static void authSql(WrapConnect connect, String user, String sql)
            throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, connect.getDbTypeLower());
        parser.setDefaultDbName(connect.getDefaultDb());
        parser.setConn(connect);
        authSql(user, connect.getAK(), parser.parseToSQLInfo());
    }

    /**
     * 对sql中的库,表,列鉴权是否允许执行
     * 列的查询请求这里不做处理,在返回结果集的时候处理
     */
    private static void authSql(Object userPriv, String dbkey, String user, SqlInfo... sqlInfos)
            throws PermissionException, SQLException {
        if (sqlInfos == null || sqlInfos.length == 0) return;
        List<String> userAuth = splitComma(userPriv);
        for (SqlInfo sqlInfo : sqlInfos) {
            String tbName = sqlInfo.getName();
            Set<String> operators = sqlInfo.getOperators();
            List<String> dbAuth = null;
            Map<String, List<String>> tbAuth = null;
            for (String operator : operators) {
                if (!userAuth.contains(operator)) {
                    dbAuth = dbAuth(dbkey, user);
                    if (!dbAuth.contains(operator)) {
                        tbAuth = tbAuth(dbkey, user, tbName);
                        List<String> tbList = tbAuth.get("priv");
                        if (!tbList.contains(operator)) throw new PermissionException("用户[" + user + "]无权限操作[" +
                                operator + "]表[" + tbName + "]");
                    }
                }
                Map<SqlInfo.PairName, Set<String>> cols = sqlInfo.getCols();
                if (cols != null) {
                    Map<String, List<String>> colAuth = colAuth(dbkey, user, tbName);
                    for (SqlInfo.PairName pairName : cols.keySet()) {
                        String colName = pairName.getName();
                        authColumn(cols.get(pairName), userAuth, dbAuth, tbAuth, colAuth.get(colName),
                                dbkey, user, tbName, colName);
                    }
                }
            }


        }
    }

    private static void authColumn(Set<String> operators, List<String> userAuth, List<String> dbAuth,
                                   Map<String, List<String>> tbAuth, List<String> colAuth,
                                   String dbkey, String user, String tbName, String colName) throws PermissionException, SQLException {
        for (String operator : operators) {
            if (!"select".equals(operator)) {
                if (!userAuth.contains(operator)) {
                    if (dbAuth == null) dbAuth = dbAuth(dbkey, user);
                    if (!dbAuth.contains(operator)) {
                        if (tbAuth == null) tbAuth = tbAuth(dbkey, user, tbName);
                        List<String> tcList = tbAuth.get("colpriv");
                        if (!tcList.contains(operator)) {
                            if (!colAuth.contains(operator))
                                throw new PermissionException("用户[" + user + "]无权限操作[" + operator + "]列[" +
                                        dbkey + "." + tbName + "." + colName + "]");
                        }

                    }
                }
            }
        }
    }

}
