package com.jdbc.bean;

import io.netty.channel.ChannelHandlerContext;

import java.sql.*;
import java.time.Instant;

import static com.handler.IOHandler.OK;
import static com.handler.IOHandler.writeShortStr;

public class WrapPrepareStatement extends WrapStatement {

    private final PreparedStatement statement;

    WrapPrepareStatement(WrapConnect wrapConnect, String id, PreparedStatement statement) {
        super(wrapConnect, id, statement);
        this.statement = statement;
    }

    public void executeQuery(ChannelHandlerContext out) throws SQLException {
        ResultSet rs = this.statement.executeQuery();
        String rsId = this.id + COUNTER.incrementAndGet();
        WrapResultSet wrs = new WrapResultSet(this, rsId, rs);
        rsMap.put(rsId, wrs);
        out.write(writeShortStr(OK, rsId));
        wrs.getMetaData(out);
        wrs.next(true, out);
    }

    public int executeUpdate() throws SQLException {
        return this.statement.executeUpdate();
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        this.statement.setNull(parameterIndex, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        this.statement.setBoolean(parameterIndex, x);
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        this.statement.setByte(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        this.statement.setShort(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        this.statement.setInt(parameterIndex, x);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        this.statement.setLong(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        this.statement.setFloat(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        this.statement.setDouble(parameterIndex, x);
    }


    public void setString(int parameterIndex, String x) throws SQLException {
        this.statement.setString(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        this.statement.setBytes(parameterIndex, x);
    }

    public void setDate(int parameterIndex, long milliseconds) throws SQLException {
        this.statement.setDate(parameterIndex, new Date(milliseconds));
    }

    public void setTime(int parameterIndex, long milliseconds) throws SQLException {
        this.statement.setTime(parameterIndex, new Time(milliseconds));
    }

    public void setTimestamp(int parameterIndex, long epochSecond, int nanos) throws SQLException {
        Timestamp ts = Timestamp.from(Instant.ofEpochSecond(epochSecond, nanos));
        this.statement.setTimestamp(parameterIndex, ts);
    }

    public void clearParameters() throws SQLException {
        this.statement.clearParameters();
    }


    public boolean execute() throws SQLException {
        return this.statement.execute();
    }

    public void addBatch() throws SQLException {
        this.statement.addBatch();
    }


    public void setNString(int parameterIndex, String value) throws SQLException {
        this.statement.setNString(parameterIndex, value);
    }

}
