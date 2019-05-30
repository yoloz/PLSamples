package com.jdbc.bean;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.handler.IOHandler.*;

public class WrapResultSet implements AutoCloseable {

    private final String id;
    private final WrapStatement wrapStatement;
    private final ResultSet resultSet;

    //colIndex,pair
    private Map<Integer, Pair> cols = new HashMap<>(1);
    //colIndex,filterValue
    private Map<Integer, String> filters = new HashMap<>(1);

    WrapResultSet(WrapStatement wrapStatement, String id, ResultSet resultSet) {
        this.wrapStatement = wrapStatement;
        this.id = id;
        this.resultSet = resultSet;

    }

    public String getCursorName() throws SQLException {
        return this.resultSet.getCursorName();
    }

    void getMetaData(ChannelHandlerContext out) throws SQLException {
        WrapConnect wrapConnect = this.wrapStatement.getWrapConnect();
        String uk = wrapConnect.getDbKey() + wrapConnect.getUser();
        String dk = uk + wrapConnect.getDbName();
        ResultSetMetaData rsMeta = this.resultSet.getMetaData();
        int colCount = rsMeta.getColumnCount();
        out.write(writeShort(colCount));
        for (int i = 1; i <= colCount; i++) {
            String colName = rsMeta.getColumnName(i);
            String tbName = rsMeta.getTableName(i);
            if (!DbAuth.selectCol(uk, dk, tbName, colName))
                cols.put(i, new Pair(0, "no permission"));
            String fv = FilterRow.filter(dk + tbName + colName);
            if (fv != null) filters.put(i, fv);

            ByteBuf buf = Unpooled.buffer();
            writeShortString(rsMeta.getCatalogName(i), buf);
            writeShortString(rsMeta.getSchemaName(i), buf);
            writeShortString(tbName, buf);
            writeShortString(rsMeta.getColumnLabel(i), buf);
            writeShortString(colName, buf);
            writeShortString(rsMeta.getColumnTypeName(i), buf);
            buf.writeInt(rsMeta.getColumnDisplaySize(i));
            buf.writeInt(rsMeta.getPrecision(i));
            buf.writeInt(rsMeta.getScale(i));
            buf.writeInt(rsMeta.getColumnType(i));
            out.write(buf);
        }
    }

    public boolean isLast() throws SQLException {
        return this.resultSet.isLast();
    }

    public boolean isFirst() throws SQLException {
        return this.resultSet.isFirst();
    }

    public void beforeFirst() throws SQLException {
        this.resultSet.beforeFirst();
    }

    public void afterLast() throws SQLException {
        this.resultSet.afterLast();
    }

    public boolean first() throws SQLException {
        return this.resultSet.first();
    }

    public boolean last() throws SQLException {
        return this.resultSet.last();
    }

    public int getRow() throws SQLException {
        return this.resultSet.getRow();
    }

    public boolean absolute(int row) throws SQLException {
        return this.resultSet.absolute(row);
    }

    public boolean relative(int rows) throws SQLException {
        return this.resultSet.relative(rows);
    }

    public void setFetchSize(int rows) throws SQLException {
        this.resultSet.setFetchSize(rows);
    }

    private int getFetchSize() throws SQLException {
        return this.resultSet.getFetchSize();
    }

    public void next(boolean first, ChannelHandlerContext out) throws SQLException {
        int fetchSize = getFetchSize();
        if (fetchSize == 0) fetchSize = this.wrapStatement.getFetchSize();
        int colCount = this.resultSet.getMetaData().getColumnCount();
        if (!first) out.write(writeByte((byte) 0x00));
        for (int i = 0; i < fetchSize; i++) {
            if (this.resultSet.next()) {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(0x7e);
                for (int j = 1; j <= colCount; j++) {

                    if (filters.containsKey(j)) {
                        String val = this.resultSet.getString(j);
                        if (filters.get(j).equals(val)) break;
                    }
                    byte[] bytes = this.resultSet.getBytes(j);

                    if (cols.containsKey(j)) {
                        Pair pair = cols.get(j);
                        if (0 == pair.code) bytes = pair.policy.getBytes(StandardCharsets.UTF_8);
                        else throw new SQLException("column control code[" + pair.code + "] is not defined");
                    }

                    if (bytes == null) buf.writeInt(~0);
                    else {
                        buf.writeInt(bytes.length);
                        buf.writeBytes(bytes);
                    }
                }
                out.write(buf);
            }
        }
        out.write(writeByte((byte) 0x7f));
    }

    @Override
    public void close() {
        try {
            if (resultSet != null) resultSet.close();
            wrapStatement.rsMap.remove(id);
        } catch (SQLException ignored) {
        }
    }

    private class Pair {

        private final Integer code; //0:select permission denied,1:mask|encode
        private final String policy;

        private Pair(Integer code, String policy) {
            this.code = code;
            this.policy = policy;
        }
    }
}
