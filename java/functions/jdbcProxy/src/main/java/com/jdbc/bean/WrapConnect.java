package com.jdbc.bean;

import com.audit.AuditEvent;
import com.audit.AuditManager;
import com.google.common.cache.*;
import com.handler.PermissionException;
import com.jdbc.sql.parser.SQLParserUtils;
import com.jdbc.sql.parser.SQLStatementParser;
import com.strategy.DSGInfo;
import com.util.Constants;
import com.util.ProxyClassLoader;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private ProxyClassLoader pcl;

    private final String rAddress;
    private final String ak;
    private final int platform_id;
    private final int appid;


    private final Connection dbConnect;
    private final String dbType;
    private String defaultDb;

    final Cache<String, WrapStatement> stmtMap = CacheBuilder.newBuilder()
            .expireAfterAccess(Constants.proxyTimeout, TimeUnit.SECONDS)
            .removalListener((RemovalListener<String, WrapStatement>) notify -> {
                if (notify.getCause() == RemovalCause.EXPIRED) {
                    notify.getValue().close();
                }
            }).build();

    @SuppressWarnings("unchecked")
    public WrapConnect(String rAddress, String ak) throws Exception {
        this.rAddress = rAddress;
        this.ak = ak;

        Map<String, Object> dsgInfo = DSGInfo.getAppInfo(ak);
        String rClient = String.valueOf(dsgInfo.get("client"));
        String rMac = String.valueOf(dsgInfo.get("mac"));
        String rProcess = String.valueOf(dsgInfo.get("process"));
        if (!rAddress.contains(rClient))
            throw new Exception("address[" + rAddress + "] has no permission to connect by " + ak);

        this.appid = Integer.parseInt(String.valueOf(dsgInfo.get("id")));
        this.platform_id = Integer.parseInt(String.valueOf(dsgInfo.get("dbms_id")).substring(0, 3));
        this.dbType = String.valueOf(dsgInfo.get("server_type")).toLowerCase();
        this.defaultDb = String.valueOf(dsgInfo.get("default_database"));

        Properties property = new Properties();
        String properties = String.valueOf(dsgInfo.get("properties"));
        if (properties != null && !properties.isEmpty()) {
            String[] props = properties.split("&");
            for (String prop : props) {
                String[] kv = prop.split("=");
                if (kv.length != 2) throw new SQLException("properties format[" + prop + "] error");
                if (!kv[0].equals("user") && !kv[0].equals("password")) property.put(kv[0], kv[1]);
            }
        }
        property.put("user", String.valueOf(dsgInfo.get("username")));
        property.put("password", String.valueOf(dsgInfo.get("password")));
        String url;
        if ("mysql".equals(dbType)) url = "jdbc" + ":" + dbType + "://" + dsgInfo.get("server_ip")
                + ":" + dsgInfo.get("server_port") + "/" + defaultDb;
        else if ("oracle".equals(dbType)) {
            url = "jdbc" + ":" + dbType + ":thin:@//" + dsgInfo.get("server_ip")
                    + ":" + dsgInfo.get("server_port") + "/" + defaultDb;
            defaultDb = property.getProperty("user");
        } else throw new SQLException("type[" + dbType + "] is not support");
        this.dbConnect = create((List<String>) dsgInfo.get("driverPath"),
                String.valueOf(dsgInfo.get("driverClass")), url, property);
        AuditManager.getInstance().audit(new AuditEvent(rAddress, ak, "createConnect",
                ak, property.toString()));
    }

    private Connection create(List<String> driverPath, String driverClass, String url,
                              Properties properties) throws SQLException {
        try {
            URL[] urls = new URL[driverPath.size()];
            Path rp = Paths.get(Constants.JPPath, "ext");
            for (int i = 0; i < driverPath.size(); i++)
                urls[i] = new URL("jar:file:" + rp.resolve(driverPath.get(i)) + "!/");
            pcl = new ProxyClassLoader(urls);
            Driver driver = (Driver) pcl.loadClass(driverClass).newInstance();
            return driver.connect(url, properties);
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

    public String getAK() {
        return ak;
    }

    public int getPlatform_id() {
        return platform_id;
    }

    public int getAppid() {
        return appid;
    }

    public String getDefaultDb() {
        return defaultDb;
    }

    public String getDbType() {
        return dbType;
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
        return stmtMap.getIfPresent(stmtId);
    }

    public WrapPrepareStatement getPrepareStatement(String stmtId) {
        return (WrapPrepareStatement) stmtMap.getIfPresent(stmtId);
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


    public String prepareStatement(String user, String sql) throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbType);
        parser.setDefaultDbName(defaultDb);
        parser.setConn(this);
        Set<String> noSelectCols = DSGInfo.checkPermission(appid, platform_id, user, parser.parseToSQLInfo());
        Map<Integer, Map<String, Object>> indexes = parser.encryptPStmtSql(user);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes, noSelectCols);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int[] columnIndexes)
            throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbType);
        parser.setDefaultDbName(defaultDb);
        parser.setConn(this);
        Set<String> noSelectCols = DSGInfo.checkPermission(appid, platform_id, user, parser.parseToSQLInfo());
        Map<Integer, Map<String, Object>> indexes = parser.encryptPStmtSql(user);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, columnIndexes);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes, noSelectCols);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, String[] columnNames)
            throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbType);
        parser.setDefaultDbName(defaultDb);
        parser.setConn(this);
        Set<String> noSelectCols = DSGInfo.checkPermission(appid, platform_id, user, parser.parseToSQLInfo());
        Map<Integer, Map<String, Object>> indexes = parser.encryptPStmtSql(user);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, columnNames);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes, noSelectCols);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int autoGeneratedKeys)
            throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbType);
        parser.setDefaultDbName(defaultDb);
        parser.setConn(this);
        Set<String> noSelectCols = DSGInfo.checkPermission(appid, platform_id, user, parser.parseToSQLInfo());
        Map<Integer, Map<String, Object>> indexes = parser.encryptPStmtSql(user);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, autoGeneratedKeys);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes, noSelectCols);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbType);
        parser.setDefaultDbName(defaultDb);
        parser.setConn(this);
        Set<String> noSelectCols = DSGInfo.checkPermission(appid, platform_id, user, parser.parseToSQLInfo());
        Map<Integer, Map<String, Object>> indexes = parser.encryptPStmtSql(user);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, resultSetType, resultSetConcurrency);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes, noSelectCols);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String prepareStatement(String user, String sql, int resultSetType, int resultSetConcurrency,
                                   int resultSetHoldability) throws SQLException, PermissionException {
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbType);
        parser.setDefaultDbName(defaultDb);
        parser.setConn(this);
        Set<String> noSelectCols = DSGInfo.checkPermission(appid, platform_id, user, parser.parseToSQLInfo());
        Map<Integer, Map<String, Object>> indexes = parser.encryptPStmtSql(user);
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
        String stmtId = generateStmt();
        WrapPrepareStatement wrs = new WrapPrepareStatement(this, stmtId, user, stmt,
                indexes, noSelectCols);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    @Override
    public void close() {
        stmtMap.asMap().values().forEach(WrapStatement::close);
        stmtMap.invalidateAll();
        try {
            if (dbConnect != null) dbConnect.close();
            if (pcl != null) pcl.close();
        } catch (SQLException | IOException ignored) {
        }
    }
}
