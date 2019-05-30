package com.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static com.handler.IOHandler.*;

public class ConnectMetaHandler {

    public static void handler(DatabaseMetaData metaData, ByteBuf src, ChannelHandlerContext out) throws SQLException {
        throw new SQLException("connectionDataMeta is developing");
//        out.write(writeCmd(OK));
    }
}
