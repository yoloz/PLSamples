package com.jdbc.bean;

import java.util.Map;

public class DataSource {

    private String keyword;
    private String type;
    private String host;
    private String driver;

    private String database;
    private String properties;

    private DataSource() {
    }

    public DataSource(Map<String, String> map) {
        setKeyword(map.getOrDefault("keyword", ""))
                .setType(map.getOrDefault("type", ""))
                .setHost(map.getOrDefault("host", ""))
                .setDriver(map.getOrDefault("driver", ""));
    }

    public String getKeyword() {
        return keyword;
    }

    private DataSource setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    public String getType() {
        return type;
    }

    private DataSource setType(String type) {
        this.type = type;
        return this;
    }

    public String getHost() {
        return host;
    }

    private DataSource setHost(String host) {
        this.host = host;
        return this;
    }

    public String getDriver() {
        return driver;
    }

    private DataSource setDriver(String driver) {
        this.driver = driver;
        return this;
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
        StringBuilder s = new StringBuilder("jdbc").append(":").append(type).append("://").append(host)
                .append("/").append(database);
        if (properties != null && !properties.isEmpty()) s.append("?").append(properties);
        return s.toString();
    }

}