package com.handler;

import com.source.Jdbc;
import com.source.Source;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class JdbcHandler {

    static Connection getConnection(String keyword, String user, String pwd, String db, String properties)
            throws SQLException {
        Jdbc jdbc = Source.mapper.get(keyword);
        if (jdbc == null) throw new SQLException("category[" + keyword + "] is not defined");
        else {
            jdbc.setDatabase(db);
            jdbc.setProperties(properties);
            try {
                Class.forName(jdbc.getDriver());
                return DriverManager.getConnection(jdbc.toUrl(), user, pwd);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.getMessage());
            }
        }
    }

    public static ResultSet query(Connection conn, String sql, Object... params)
            throws SQLException {
        if (conn == null) throw new SQLException("Null connection");
        if (sql == null) throw new SQLException("Null SQL statement");
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(sql);
        if (params != null) for (int i = 0; i < params.length; i++)
            stmt.setObject(i + 1, params[i]);
        return stmt.executeQuery();
    }

    public static int update(Connection conn, String sql, Object... params)
            throws SQLException {
        if (conn == null) throw new SQLException("Null connection");
        if (sql == null) throw new SQLException("Null SQL statement");
        PreparedStatement stmt = null;
        stmt = conn.prepareStatement(sql);
        if (params != null) for (int i = 0; i < params.length; i++)
            stmt.setObject(i + 1, params[i]);
        return stmt.executeUpdate();
    }

    static void wrapResultSetMeta(ResultSetMetaData metaData, int index, ByteBuf buf) throws SQLException {
        byte[] catalogName = metaData.getCatalogName(index).getBytes(StandardCharsets.UTF_8);
        buf.writeShort(catalogName.length);
        buf.writeBytes(catalogName);
        byte[] schemaName = metaData.getSchemaName(index).getBytes(StandardCharsets.UTF_8);
        buf.writeShort(schemaName.length);
        buf.writeBytes(schemaName);
        byte[] tableName = metaData.getTableName(index).getBytes(StandardCharsets.UTF_8);
        buf.writeShort(tableName.length);
        buf.writeBytes(tableName);
        byte[] columnLabel = metaData.getColumnLabel(index).getBytes(StandardCharsets.UTF_8);
        buf.writeShort(columnLabel.length);
        buf.writeBytes(columnLabel);
        byte[] columnName = metaData.getColumnName(index).getBytes(StandardCharsets.UTF_8);
        buf.writeShort(columnName.length);
        buf.writeBytes(columnName);
        byte[] columnTypeName = metaData.getColumnTypeName(index).getBytes(StandardCharsets.UTF_8);
        buf.writeShort(columnTypeName.length);
        buf.writeBytes(columnTypeName);
        //number
        buf.writeInt(metaData.getColumnDisplaySize(index));
        buf.writeInt(metaData.getPrecision(index));
        buf.writeInt(metaData.getScale(index));
        buf.writeInt(metaData.getColumnType(index));
    }

}
