package com.handler;

import com.source.Connect;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class IOHandler {


    /**
     * request jdbc connection
     */
    public static Connect requestConnect(ByteBuf buf) throws SQLException {
        int cl = buf.readUnsignedShort();
        String keyWord = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), cl), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + cl);
        int ul = buf.readUnsignedShort();
        String userName = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), ul), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + ul);
        int pl = buf.readUnsignedShort();
        String pwd = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), pl), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + pl);
        int dbl = buf.readUnsignedShort();
        String database = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), dbl), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + dbl);
        int prl = buf.readInt();
        String properties = new String(ByteBufUtil.getBytes(buf, buf.readerIndex(), prl), StandardCharsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + prl);
        return JdbcHandler.getConnect(keyWord, userName, pwd, database, properties);
    }

    /**
     * response resultSetMeta
     */
    public static void rsMetaOkP(ChannelHandlerContext ctx, ResultSetMetaData rsMeta)
            throws SQLException {
        ByteBuf buf = Unpooled.buffer();
        for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
            JdbcHandler.wrapResultSetMeta(rsMeta, i, buf);
        }
        ctx.write(buf);
    }

    public static void rsRowOkP(ChannelHandlerContext ctx, ResultSet rs)
            throws SQLException {
        while (rs.next()) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(0x7e);
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                byte[] bytes = rs.getBytes(i);
                if (bytes == null) buf.writeInt(~0);
                else {
                    buf.writeInt(bytes.length);
                    buf.writeBytes(bytes);
                }
            }
            ctx.write(buf);
        }
        ByteBuf buf = Unpooled.buffer(1);
        buf.writeByte(0x7f);
        ctx.write(buf);
    }

    /**
     * response DML&DDL
     */
    public static void updateOkP(ChannelHandlerContext ctx, int code) {
        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte(0x00);
        buf.writeInt(code);
        ctx.write(buf);
    }

    /**
     * response connect
     */
    public static void connOkP(ChannelHandlerContext ctx) {
        ByteBuf buf = Unpooled.buffer(1);
        buf.writeByte(0x00);
        ctx.write(buf);
    }

    /**
     * errorP packet
     *
     * @param error errorP msg
     */
    public static void errorP(ChannelHandlerContext ctx, String error) {
        byte[] e = error.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(5 + e.length);
        buf.writeByte(0x01);
        buf.writeInt(e.length);
        buf.writeBytes(e);
        ctx.write(buf);
    }

}
