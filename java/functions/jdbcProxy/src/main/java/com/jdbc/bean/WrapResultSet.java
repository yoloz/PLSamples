package com.jdbc.bean;

import com.strategy.DSGInfo;
import com.strategy.MaskLogic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static com.handler.IOHandler.*;

public class WrapResultSet implements AutoCloseable {

    private final String id;
    private final WrapStatement wrapStatement;
    private final ResultSet resultSet;

    private Set<String> noSelectCols;

    //colIndex,pair
    private Map<Integer, Pair> colFilter = new HashMap<>(1);
    //colIndex,filterValue
    private Map<Integer, String> rowFilter = new HashMap<>(1);

    WrapResultSet(WrapStatement wrapStatement, String id, ResultSet resultSet, Set<String> noSelectCols) {
        this.wrapStatement = wrapStatement;
        this.id = id;
        this.resultSet = resultSet;
        this.noSelectCols = noSelectCols;
    }

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

    private boolean canSelect(Map<String, List<String>> map, String dbName, String tableName,
                              String colName) {
        if (map == null) return false;
        String table = dbName + "." + tableName;
        String cn = dbName + "." + tableName + "." + colName;
        List<String> col = map.get("col");
        List<String> db = map.get("db");
        List<String> tb = map.get("tb");
        if (db == null || !db.contains(dbName)) {
            if (tb == null || !tb.contains(table)) {
                return (col != null) && col.contains(cn);
            }
        }
        return true;
    }

    void getMetaData(ChannelHandlerContext out) throws SQLException {
        WrapConnect connect = wrapStatement.getWrapConnect();
        Map<String, List<String>> selectPermission = null;
        if (noSelectCols == null) selectPermission = DSGInfo.getSelectPermission(connect.getAppid(),
                connect.getPlatform_id(), wrapStatement.getUser());
        Map<String, Map<String, String>> filterRows = new HashMap<>();
        Map<String, Map<String, Map<String, Object>>> mask_policy = new HashMap<>();
        ResultSetMetaData rsMeta = this.resultSet.getMetaData();
        int colCount = rsMeta.getColumnCount();
        out.write(writeShort(colCount));
        //todo 兼容不同数据库,如oracle的列名默认大写
        for (int i = 1; i <= colCount; i++) {
            String dbName = rsMeta.getCatalogName(i);
            if (dbName == null || dbName.isEmpty()) dbName = rsMeta.getSchemaName(i);
            String colName = rsMeta.getColumnName(i);
            String tbName = rsMeta.getTableName(i);
            String cn = dbName + "." + tbName + "." + colName;
            if (!filterRows.containsKey(tbName)) filterRows.put(tbName, DSGInfo.filterRow(connect.getAK(),
                    wrapStatement.getUser(), tbName));
            if (!mask_policy.containsKey(tbName)) mask_policy.put(tbName, MaskLogic.getMaskPolicy(connect.getAK(),
                    wrapStatement.getUser(), connect.getDefaultDb(), tbName));

//            for (String s : mask_policy.keySet()) {
//                System.out.println(s + "==>");
//                mask_policy.get(s).forEach((k, v) -> System.out.println(k));
//            }

            if (!mask_policy.isEmpty() && mask_policy.get(tbName).containsKey(colName)) {
                Map<String, Object> policy = mask_policy.get(tbName).get(colName);
                colFilter.put(i, new Pair((int) policy.get("type"), policy));
            }
            if (noSelectCols != null) {
                if (noSelectCols.contains(cn)) colFilter.put(i, new Pair(0));
            } else {
                if (!canSelect(selectPermission, dbName, tbName, colName)) colFilter.put(i, new Pair(0));
            }

            if (filterRows.containsKey(tbName) && filterRows.get(tbName).containsKey(colName)) {
                String val = filterRows.get(tbName).get(colName);
                rowFilter.put(i, val);
//                switch (rsMeta.getColumnType(i)) {
//                    case -6:
//                    case 5:
//                    case 4:
//                        rowFilter.put(i, Integer.parseInt(val));
//                        break;
//                    case -5:
//                        rowFilter.put(i, Long.valueOf(val));
//                        break;
//                    case 6:
//                        rowFilter.put(i, Float.parseFloat(val));
//                        break;
//                    case 8:
//                        rowFilter.put(i, Double.parseDouble(val));
//                        break;
//                    default:
//                        rowFilter.put(i, val);
//                }
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
        ResultSetMetaData rsmd = this.resultSet.getMetaData();
        if (!first) out.write(writeByte((byte) 0x00));
        while (fetchSize > 0 && this.resultSet.next()) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(0x7e);
            for (int j = 1; j <= rsmd.getColumnCount(); j++) {
                if (rowFilter.containsKey(j)) {
                    String val = String.valueOf(this.resultSet.getObject(j));
                    if (rowFilter.get(j).equals(val)) {
                        buf.clear();
                        break;
                    }
                }
                byte[] bytes;
                if (rsmd.getColumnType(j) == 2)
                    bytes = this.resultSet.getBigDecimal(j).toPlainString().getBytes(StandardCharsets.UTF_8);
                else bytes = this.resultSet.getBytes(j);
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
