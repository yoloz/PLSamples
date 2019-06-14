package com.jdbc.bean;

import com.handler.UserHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

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

    public WrapStatement getWrapStatement() {
        return wrapStatement;
    }

    public String getCursorName() throws SQLException {
        return this.resultSet.getCursorName();
    }

    private boolean canRead(List<String> userAuth, List<String> dbAuth, List<String> tbAuth, List<String> colAuth) {
        String operator = "select";
        if (!userAuth.contains(operator)) {
            if (!dbAuth.contains(operator)) {
                if (!tbAuth.contains(operator)) {
                    if (colAuth == null) return false;
                    return colAuth.contains(operator);
                }
            }
        }
        return true;
    }

    void getMetaData(ChannelHandlerContext out) throws SQLException {
        WrapConnect connect = wrapStatement.getWrapConnect();
        List<String> userAuth = UserHandler.splitComma(UserHandler.userAuth(wrapStatement.getUser()));
        List<String> dbAuth = UserHandler.dbAuth(connect.getDbKey(), wrapStatement.getUser());
        Map<String, List<String>> tbAuth = new HashMap<>();
        Map<String, Map<String, List<String>>> colAuth = new HashMap<>();
        Map<String, Map<String, String>> filterRows = new HashMap<>();

        ResultSetMetaData rsMeta = this.resultSet.getMetaData();
        int colCount = rsMeta.getColumnCount();
        out.write(writeShort(colCount));
        for (int i = 1; i <= colCount; i++) {
            String colName = rsMeta.getColumnName(i);
            String tbName = rsMeta.getTableName(i);
            if (!tbAuth.containsKey(tbName)) tbAuth.put(tbName, UserHandler.tbAuth(connect.getDbKey(),
                    wrapStatement.getUser(), tbName).get("colpriv"));
            if (!colAuth.containsKey(tbName)) colAuth.put(tbName, UserHandler.colAuth(connect.getDbKey(),
                    wrapStatement.getUser(), tbName));
            if (!filterRows.containsKey(tbName)) filterRows.put(tbName, UserHandler.filterRow(connect.getDbKey(),
                    wrapStatement.getUser(), tbName));

            if (!canRead(userAuth, dbAuth, tbAuth.get(tbName), colAuth.get(tbName).get(colName)))
                cols.put(i, new Pair(0));
            if (filterRows.containsKey(tbName) && filterRows.get(tbName).containsKey(colName)) {
                filters.put(i, filterRows.get(tbName).get(colName));
            }

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
        while (fetchSize > 0 && this.resultSet.next()) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(0x7e);
            for (int j = 1; j <= colCount; j++) {
                if (filters.containsKey(j)) {
                    String val = this.resultSet.getString(j);
                    if (filters.get(j).equals(val)) {
                        buf.clear();
                        break;
                    }
                }
                byte[] bytes = this.resultSet.getBytes(j);
                if (cols.containsKey(j)) {
                    Pair pair = cols.get(j);
                    if (0 == pair.code) bytes = null;
                    else throw new SQLException("column control code[" + pair.code + "] is not defined");
                }
                if (bytes == null) buf.writeInt(~0);
                else {
                    buf.writeInt(bytes.length);
                    buf.writeBytes(bytes);
                }
            }
            out.write(buf);
            fetchSize--;
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

        private Pair(Integer code) {
            this(code, null);
        }

        private Pair(Integer code, String policy) {
            this.code = code;
            this.policy = policy;
        }
    }
}
