package com.handler;

import com.jdbc.bean.WrapPrepareStatement;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.sql.*;

import static com.handler.IOHandler.*;

public class PrepareStatementHandler {

    public static void handler(WrapPrepareStatement statement, ByteBuf src, ChannelHandlerContext out)
            throws SQLException {
        String mName = IOHandler.readByteLen(src);
        if ("executeQuery".equals(mName)) {
            statement.executeQuery(out);
        } else if ("executeUpdate".equals(mName)) {
            out.write(writeInt(OK, statement.executeUpdate()));
        } else if ("setNull".equals(mName)) {
            statement.setNull(src.readInt(), src.readInt());
            out.write(writeByte(OK));
        } else if ("setBoolean".equals(mName)) {
            statement.setBoolean(src.readInt(), "true".equals(readByteLen(src)));
            out.write(writeByte(OK));
        } else if ("setByte".equals(mName)) {
            statement.setByte(src.readInt(), src.readByte());
            out.write(writeByte(OK));
        } else if ("setShort".equals(mName)) {
            statement.setShort(src.readInt(), src.readShort());
            out.write(writeByte(OK));
        } else if ("setInt".equals(mName)) {
            statement.setInt(src.readInt(), src.readInt());
            out.write(writeByte(OK));
        } else if ("setLong".equals(mName)) {
            statement.setLong(src.readInt(), src.readLong());
            out.write(writeByte(OK));
        } else if ("setFloat".equals(mName)) {
            statement.setFloat(src.readInt(), src.readFloat());
            out.write(writeByte(OK));
        } else if ("setDouble".equals(mName)) {
            statement.setDouble(src.readInt(), src.readDouble());
            out.write(writeByte(OK));
        } else if ("setString".equals(mName)) {
            statement.setString(src.readInt(), readIntLen(src));
            out.write(writeByte(OK));
        } else if ("setBytes".equals(mName)) {
            statement.setBytes(src.readInt(), readBytes(src.readInt(), src));
            out.write(writeByte(OK));
        } else if ("setDate".equals(mName)) {
            statement.setDate(src.readInt(), src.readLong());
            out.write(writeByte(OK));
        } else if ("setTime".equals(mName)) {
            statement.setTime(src.readInt(), src.readLong());
            out.write(writeByte(OK));
        } else if ("setTimestamp".equals(mName)) {
            statement.setTimestamp(src.readInt(), src.readLong(), src.readInt());
            out.write(writeByte(OK));
        } else if ("clearParameters".equals(mName)) {
            statement.clearParameters();
            out.write(writeByte(OK));
        } else if ("execute".equals(mName)) {
            out.write(writeShortStr(OK, statement.execute() ? "true" : "false"));
        } else if ("addBatch".equals(mName)) {
            statement.addBatch();
            out.write(writeByte(OK));
        } else if ("setNString".equals(mName)) {
            statement.setNString(src.readInt(), readIntLen(src));
            out.write(writeByte(OK));
        } else throw new SQLException("statementMethod[" + mName + "] is not support");
    }

}
