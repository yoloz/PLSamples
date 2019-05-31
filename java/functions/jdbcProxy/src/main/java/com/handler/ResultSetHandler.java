package com.handler;

import com.jdbc.bean.WrapResultSet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.sql.SQLException;

import static com.handler.IOHandler.*;

public class ResultSetHandler {

    public static void handler(WrapResultSet resultSet, ByteBuf src, ChannelHandlerContext out)
            throws SQLException {
        String mName = IOHandler.readByteLen(src);
        if ("getCursorName".equals(mName)) {
            out.write(writeShortStr(OK, resultSet.getCursorName()));
        } else if ("isLast".equals(mName)) {
            out.write(writeShortStr(OK, resultSet.isLast() ? "true" : "false"));
        } else if ("beforeFirst".equals(mName)) {
            resultSet.beforeFirst();
            out.write(writeByte(OK));
        } else if ("afterLast".equals(mName)) {
            resultSet.afterLast();
            out.write(writeByte(OK));
        } else if ("first".equals(mName)) {
            out.write(writeShortStr(OK, resultSet.first() ? "true" : "false"));
        } else if ("last".equals(mName)) {
            out.write(writeShortStr(OK, resultSet.last() ? "true" : "false"));
        } else if ("getRow".equals(mName)) {
            out.write(writeInt(OK, resultSet.getRow()));
        } else if ("absolute".equals(mName)) {
            boolean bool = resultSet.absolute(src.readInt());
            out.write(writeShortStr(OK, bool ? "true" : "false"));
        } else if ("relative".equals(mName)) {
            boolean bool = resultSet.relative(src.readInt());
            out.write(writeShortStr(OK, bool ? "true" : "false"));
        } else if ("setFetchSize".equals(mName)) {
            resultSet.setFetchSize(src.readInt());
            out.write(writeByte(OK));
        } else if ("next".equals(mName)) {
            resultSet.next(false, out);
        } else if ("close".equals(mName)) {
            resultSet.close();
            out.write(writeByte(OK));
        } else throw new SQLException("statementMethod[" + mName + "] is not support");
    }
}