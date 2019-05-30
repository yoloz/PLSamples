package com.jdbc.bean;

import com.JPServer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DbSource {

    //todo guava load cache from database
    static final ConcurrentMap<String, DbSource> cache = new ConcurrentHashMap<>();

    private String dbKey;
    private String dbType;
    private String dbHost;
    private String driverClass;
    private String driverPath;

    private String database;
    private String properties;

    static {
        try (InputStream in = Files.newInputStream(Paths.get(JPServer.JPPath, "conf", "dbSource.yaml"))) {
            ArrayList list = new Yaml().loadAs(in, ArrayList.class);
            for (Object o : list) {
                DbSource dbSource = new DbSource(o);
                cache.put(dbSource.getDbKey(), dbSource);
            }
        } catch (IOException e) {
            System.err.println("loading dbSource error " + e);
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private DbSource(Object obj) {
        Map<String, String> map = (Map<String, String>) obj;
        setDbKey(Objects.requireNonNull(map.get("dbKey"), "dbKey is null"))
                .setDbType(Objects.requireNonNull(map.get("dbType"), "dbType is null"))
                .setDbHost(Objects.requireNonNull(map.get("dbHost"), "dbHost is null"))
                .setDriverClass(Objects.requireNonNull(map.get("driverClass"), "driverClass is null"))
                .setDriverPath(Objects.requireNonNull(map.get("driverPath"), "driverPath is null"));
    }

    public String getDbKey() {
        return dbKey;
    }

    private DbSource setDbKey(String dbKey) {
        this.dbKey = dbKey;
        return this;
    }

    public String getDbType() {
        return dbType;
    }

    private DbSource setDbType(String dbType) {
        this.dbType = dbType;
        return this;
    }

    public String getDbHost() {
        return dbHost;
    }

    private DbSource setDbHost(String dbHost) {
        this.dbHost = dbHost;
        return this;
    }

    public String getDriverClass() {
        return driverClass;
    }

    private DbSource setDriverClass(String driverClass) {
        this.driverClass = driverClass;
        return this;
    }

    public String getDriverPath() {
        return driverPath;
    }

    private void setDriverPath(String driverPath) {
        this.driverPath = driverPath;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getProperties() {
        return properties;
    }

    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String toUrl() {
        StringBuilder s = new StringBuilder("jdbc").append(":").append(dbType).append("://").append(dbHost)
                .append("/").append(database);
        if (properties != null && !properties.isEmpty()) s.append("?").append(properties);
        return s.toString();
    }

}