package com.jdbc.bean;


import com.handler.PermissionException;
import com.handler.UserHandler;
import com.jdbc.sql.parser.SQLParserUtils;
import com.jdbc.sql.parser.SQLStatementParser;
import com.util.Constants;
import com.util.InnerDb;

import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.handler.IOHandler.md5;

public class WrapConnect implements Closeable {

    private final AtomicInteger COUNTER = new AtomicInteger(1);

    private final int defaultFetchSize = 1000;

    private final String rAddress;
    private final Connection dbConnect;
    private final String ak;
    private final Info info;

    private long timestamp;


    ConcurrentMap<String, WrapStatement> stmtMap = new ConcurrentHashMap<>(1);

    public WrapConnect(String rAddress, String ak) throws SQLException {
        this.rAddress = rAddress;
        this.ak = ak;
        Properties property = new Properties();
//        if (properties != null && !properties.isEmpty()) {
//            String[] props = properties.split("&");
//            for (String prop : props) {
//                String[] kv = prop.split("=");
//                if (kv.length != 2) throw new SQLException("properties format[" + prop + "] error");
//                if (kv[0].equals("user")) this.user = kv[1];
//                else if (kv[0].equals("password")) this.pwd = kv[1];
//                else property.put(kv[0], kv[1]);
//            }
//        }
        this.info = new Info(ak, property);
        this.dbConnect = initConnection();
        this.timestamp = System.currentTimeMillis();
//        AuditManager.getInstance().audit(new AuditEvent(rAddress, user, "createConnect",
//                dbKey, property.toString()));
    }

    private Connection initConnection()
            throws SQLException {
        try {
            URL u = new URL("jar:file:" + info.getDriverPath() + "!/");
            URLClassLoader ucl = new URLClassLoader(new URL[]{u});
            Driver driver = (Driver) Class.forName(info.getDriverClass(), true, ucl).newInstance();
            return driver.connect(info.toUrl(), info.properties);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException
                | MalformedURLException e) {
            throw new SQLException(e);
        }
    }

    public String getAddress() {
        return rAddress;
    }

    private String generateStmt() {
        return md5(rAddress + COUNTER.incrementAndGet());
    }

    public void updateTime(long ts) {
        timestamp = ts;
    }

    public String getAK() {
        return ak;
    }

    public String getDefaultDb() {
        return info.dbName;
    }

    public String getDbTypeLower() {
        return info.getLowerType();
    }

    public Connection getDbConnect() {
        return dbConnect;
    }

    public void setCatalog(String catalog) throws SQLException {
        this.dbConnect.setCatalog(catalog);
    }

    public void setSchema(String schema) throws SQLException {
        this.dbConnect.setSchema(schema);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.dbConnect.setAutoCommit(autoCommit);
    }

    public void commit() throws SQLException {
        this.dbConnect.commit();
    }

    public void rollback() throws SQLException {
        this.dbConnect.rollback();
    }

    public WrapStatement getStatement(String stmtId) {
        return stmtMap.get(stmtId);
    }

    public WrapPrepareStatement getPrepareStatement(String stmtId) {
        return (WrapPrepareStatement) stmtMap.get(stmtId);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return this.dbConnect.getMetaData();
    }

    public String createStatement(String user) throws SQLException {
        Statement stmt = this.dbConnect.createStatement();
        String stmtId = generateStmt();
        WrapStatement wrs = new WrapStatement(this, stmtId, user, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createStatement(String user, int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement stmt = this.dbConnect.createStatement(resultSetType, resultSetConcurrency);
        String stmtId = generateStmt();
        WrapStatement wrs = new WrapStatement(this, stmtId, user, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createStatement(String user, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        Statement stmt = this.dbConnect.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability);
        String stmtId = generateStmt();
        WrapStatement wrs = new WrapStatement(this, stmtId, user, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    private Map<Integer, Map<String, Object>> getEncryptIndexes(String user, String sql) throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, info.getLowerType());
        parser.setDefaultDbName(info.dbName);
        parser.setConn(this);
        List<SqlInfo> list = parser.parseToSQLInfo();
        UserHandler.authSql(user.isEmpty() ? ak : user, ak, list);
        return parser.encryptPStmtSql(user);
    }

    public String prepareStatement(String user, String sql) throws SQLException, PermissionException {
        Map<Integer, Map<String, Object>> indexes = getEncryptIndexes(user, sql);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int[] columnIndexes)
            throws SQLException, PermissionException {
        Map<Integer, Map<String, Object>> indexes = getEncryptIndexes(user, sql);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, columnIndexes);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, String[] columnNames)
            throws SQLException, PermissionException {
        Map<Integer, Map<String, Object>> indexes = getEncryptIndexes(user, sql);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, columnNames);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int autoGeneratedKeys)
            throws SQLException, PermissionException {
        Map<Integer, Map<String, Object>> indexes = getEncryptIndexes(user, sql);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, autoGeneratedKeys);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException, PermissionException {
        Map<Integer, Map<String, Object>> indexes = getEncryptIndexes(user, sql);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, resultSetType, resultSetConcurrency);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int resultSetType, int resultSetConcurrency,
                                   int resultSetHoldability) throws SQLException, PermissionException {
        Map<Integer, Map<String, Object>> indexes = getEncryptIndexes(user, sql);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    @Override
    public void close() {
        if (!stmtMap.isEmpty()) stmtMap.values().forEach(WrapStatement::close);
        stmtMap.clear();
        try {
            if (dbConnect != null) dbConnect.close();
        } catch (SQLException ignored) {
        }
    }

    public boolean isTimeout() {
        long current = System.currentTimeMillis();
        long timeout = Constants.proxyTimeout * 1000;
        if (current - timestamp >= timeout) {
            close();
            return true;
        } else {
            stmtMap.forEach((k, v) -> {
                if (current - v.timestamp > timeout) {
                    v.close();
                    stmtMap.remove(k);
                }
            });
            return false;
        }
    }

    private class Info {

        private final String type;
        private final String host;
        private final String dbName;
        private final String driverClass;
        private final String driverPath;

        private final Properties properties;


        private Info(String dbKey, Properties properties) throws SQLException {
            String sql = "select * from proxydb where id=?";
            Map<String, Object> map = InnerDb.get(sql, dbKey);
            if (map == null || map.isEmpty()) throw new SQLException("db[" + dbKey + "] is not exit");
            Object type = Objects.requireNonNull(map.get("type"), "dbtype is null");
            this.type = String.valueOf(type);
            Object host = Objects.requireNonNull(map.get("host"), "dbhost is null");
            Object port = Objects.requireNonNull(map.get("port"), "dbport is null");
            this.host = host + ":" + port;
            Object dbName = map.get("dbname");
            this.dbName = dbName == null ? null : String.valueOf(dbName);
            Object dbUser = Objects.requireNonNull(map.get("dbuser"), "dbuser is null");
            Object userPwd = Objects.requireNonNull(map.get("userpwd"), "userpwd is null");
            Object driverClass = Objects.requireNonNull(map.get("driverclass"), "driverclass is null");
            this.driverClass = String.valueOf(driverClass);
            Object driverPath = Objects.requireNonNull(map.get("driverpath"), "driverpath is null");
            this.driverPath = String.valueOf(driverPath);
            this.properties = new Properties(properties);
            this.properties.put("user", String.valueOf(dbUser));
            this.properties.put("password", String.valueOf(userPwd));
        }

        private String getLowerType() {
            return type.toLowerCase();
        }

        private String getDriverClass() {
            return driverClass;
        }

        private String getDriverPath() {
            return driverPath;
        }

        private String toUrl() {
            return "jdbc" + ":" + type + "://" + host + "/" + dbName;
        }

    }
}
