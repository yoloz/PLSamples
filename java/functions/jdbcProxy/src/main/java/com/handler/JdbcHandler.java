package com.handler;

import com.source.Connect;
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

    static Connect getConnect(String keyword, String user, String pwd, String db, String properties)
            throws SQLException {
        Jdbc jdbc = Source.mapper.get(keyword);
        if (jdbc == null) throw new SQLException("category[" + keyword + "] is not defined");
        else {
            jdbc.setDatabase(db);
            jdbc.setProperties(properties);
            try {
                Class.forName(jdbc.getDriver());
                Connection connection = DriverManager.getConnection(jdbc.toUrl(), user, pwd);
                return new Connect(connection, db, user);
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
        wrapString2Bytes(metaData.getCatalogName(index), buf);
        wrapString2Bytes(metaData.getSchemaName(index), buf);
        wrapString2Bytes(metaData.getTableName(index), buf);
        wrapString2Bytes(metaData.getColumnLabel(index), buf);
        wrapString2Bytes(metaData.getColumnName(index), buf);
        wrapString2Bytes(metaData.getColumnTypeName(index), buf);
        //number
        buf.writeInt(metaData.getColumnDisplaySize(index));
        buf.writeInt(metaData.getPrecision(index));
        buf.writeInt(metaData.getScale(index));
        buf.writeInt(metaData.getColumnType(index));
    }

    private static void wrapString2Bytes(String str, ByteBuf buf) {
        if (str == null) buf.writeShort(~0);
        else {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            buf.writeShort(bytes.length);
            buf.writeBytes(bytes);
        }
    }

}
