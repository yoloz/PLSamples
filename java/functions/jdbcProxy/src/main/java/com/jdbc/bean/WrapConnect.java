package com.jdbc.bean;


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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WrapConnect implements Closeable {

    private final AtomicInteger COUNTER = new AtomicInteger(1);

    private final int defaultFetchSize = 1000;

    private final String id;
    private final Connection dbConnect;
    private final String dbKey;
    private final String dbUser;
    private String user; //初始化连接的时候传递的是dbUser,可能会被重置为应用用户

    ConcurrentMap<String, WrapStatement> stmtMap = new ConcurrentHashMap<>(1);

    public WrapConnect(String id, String dbKey, String dbUser, String dbPwd, String dbName,
                       String properties) throws SQLException {
        this.id = id;
        this.dbKey = dbKey;
        this.dbUser = dbUser;
        this.dbConnect = initConnection(dbUser, dbPwd, dbName, properties);
    }

    private Connection initConnection(String dbUser, String dbPwd, String dbName, String properties)
            throws SQLException {
        DbSource dbSource = DbSource.cache.get(dbKey);
        if (dbSource == null) throw new SQLException("dbSource[" + dbKey + "] is not exist");
        else {
            dbSource.setDatabase(dbName);
            dbSource.setProperties(properties);
            try {
                URL u = new URL("jar:file:" + dbSource.getDriverPath() + "!/");
                URLClassLoader ucl = new URLClassLoader(new URL[]{u});
                Driver driver = (Driver) Class.forName(dbSource.getDriverClass(), true, ucl).newInstance();
                Properties pro = new Properties();
                pro.setProperty("user", dbUser);
                pro.setProperty("password", dbPwd);
                return driver.connect(dbSource.toUrl(), pro);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | MalformedURLException e) {
                throw new SQLException(e);
            }
        }
    }

    public String getDbKey() {
        return dbKey;
    }

    public String getDbName() {
        return DbSource.cache.get(dbKey).getDatabase();
    }

    public String getDbTypeLower() {
        return DbSource.cache.get(dbKey).getDbType().toLowerCase();
    }

    public Connection getDbConnect() {
        return dbConnect;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser() {
        return user == null ? getDbUser() : user;
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

    public DatabaseMetaData getMetaData() throws SQLException {
        return this.dbConnect.getMetaData();
    }

    public String createStatement() throws SQLException {
        Statement stmt = this.dbConnect.createStatement();
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement stmt = this.dbConnect.createStatement(resultSetType, resultSetConcurrency);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        Statement stmt = this.dbConnect.createStatement(resultSetType, resultSetConcurrency,
                resultSetHoldability);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createPreparedStatement(String sql) throws SQLException {
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createPreparedStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, columnIndexes);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createPreparedStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, columnNames);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createPreparedStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, autoGeneratedKeys);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createPreparedStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, resultSetType, resultSetConcurrency);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
        wrs.setFetchSize(defaultFetchSize);
        stmtMap.put(stmtId, wrs);
        return stmtId;
    }

    public String createPreparedStatement(String sql, int resultSetType, int resultSetConcurrency,
                                          int resultSetHoldability) throws SQLException {
        PreparedStatement stmt = this.dbConnect.prepareStatement(sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
        String stmtId = this.id + COUNTER.incrementAndGet();
        WrapStatement wrs = new WrapStatement(this, stmtId, stmt);
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
}
