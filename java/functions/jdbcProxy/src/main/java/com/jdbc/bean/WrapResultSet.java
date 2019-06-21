package com.jdbc.bean;

import com.handler.UserHandler;
import com.mask.MaskLogic;
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
    private Map<Integer, Pair> colFilter = new HashMap<>(1);
    //colIndex,filterValue
    private Map<Integer, String> rowFilter = new HashMap<>(1);

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
        List<String> dbAuth = UserHandler.dbAuth(connect.getAK(), wrapStatement.getUser());
        Map<String, List<String>> tbAuth = new HashMap<>();
        Map<String, Map<String, List<String>>> colAuth = new HashMap<>();
        Map<String, Map<String, String>> filterRows = new HashMap<>();

        ResultSetMetaData rsMeta = this.resultSet.getMetaData();
        int colCount = rsMeta.getColumnCount();
        out.write(writeShort(colCount));
        for (int i = 1; i <= colCount; i++) {
            String colName = rsMeta.getColumnName(i);
            String tbName = rsMeta.getTableName(i);
            if (!tbAuth.containsKey(tbName)) tbAuth.put(tbName, UserHandler.tbAuth(connect.getAK(),
                    wrapStatement.getUser(), tbName).get("colpriv"));
            if (!colAuth.containsKey(tbName)) colAuth.put(tbName, UserHandler.colAuth(connect.getAK(),
                    wrapStatement.getUser(), tbName));
            if (!filterRows.containsKey(tbName)) filterRows.put(tbName, UserHandler.filterRow(connect.getAK(),
                    wrapStatement.getUser(), tbName));
            Map<String, Object> mask_policy = MaskLogic.getMaskPolicy(connect.getAK(), connect.getDefaultDb(),
                    tbName, colName, wrapStatement.getUser());

            if (mask_policy != null) colFilter.put(i, new Pair((int) mask_policy.get("type"), mask_policy));
            if (!canRead(userAuth, dbAuth, tbAuth.get(tbName), colAuth.get(tbName).get(colName)))
                colFilter.put(i, new Pair(0));
            if (filterRows.containsKey(tbName) && filterRows.get(tbName).containsKey(colName)) {
                rowFilter.put(i, filterRows.get(tbName).get(colName));
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

    @SuppressWarnings("unchecked")
    public void next(boolean first, ChannelHandlerContext out) throws SQLException {
        int fetchSize = getFetchSize();
        if (fetchSize == 0) fetchSize = this.wrapStatement.getFetchSize();
        int colCount = this.resultSet.getMetaData().getColumnCount();
        if (!first) out.write(writeByte((byte) 0x00));
        while (fetchSize > 0 && this.resultSet.next()) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(0x7e);
            for (int j = 1; j <= colCount; j++) {
                if (rowFilter.containsKey(j)) {
                    String val = this.resultSet.getString(j);
                    if (rowFilter.get(j).equals(val)) {
                        buf.clear();
                        break;
                    }
                }
                byte[] bytes = this.resultSet.getBytes(j);
                if (colFilter.containsKey(j)) {
                    Pair pair = colFilter.get(j);
                    if (0 == pair.code) bytes = null;
                    else if (1 == pair.code) bytes = MaskLogic.getMaskResult(bytes, (Map<String, Object>) pair.policy);
                    else if (2 == pair.code) bytes = MaskLogic.decrypt(bytes, (Map<String, Object>) pair.policy);
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

        private final Integer code; //0:select permission denied,1:mask,2:decrypt
        private final Object policy;

        private Pair(Integer code) {
            this(code, null);
        }

        private Pair(Integer code, Object policy) {
            this.code = code;
            this.policy = policy;
        }
    }
}
