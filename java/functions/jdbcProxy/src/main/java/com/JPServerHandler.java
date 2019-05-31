package com;

import com.handler.*;
import com.jdbc.bean.WrapConnect;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.handler.IOHandler.*;


@ChannelHandler.Sharable
public class JPServerHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger = Logger.getLogger(JPServerHandler.class);
    //<address,connect>
    private final ConcurrentMap<String, WrapConnect> connects = new ConcurrentHashMap<>();


    @Override
    public void channelRead(ChannelHandlerContext out, Object obj) {
        String address = out.channel().remoteAddress().toString();
        ByteBuf src = (ByteBuf) obj;
        while (src.isReadable()) {
            short cmd = src.readUnsignedByte();
            try {
                switch (cmd) {
                    case 2:
                        String dbKey = readShortLen(src);
                        String userName = readShortLen(src);
                        String pwd = readShortLen(src);
                        String database = readShortLen(src);
                        String properties = readIntLen(src);
                        WrapConnect conn = new WrapConnect(address, dbKey, userName, pwd,
                                database, properties);
                        if (connects.containsKey(address)) closeConn(address);
                        connects.put(address, conn);
                        out.write(writeByte(OK));
                        break;
                    case 3:
                        ConnectHandler.handler(connects.get(address), src, out);
                        break;
                    case 4:
                        ConnectMetaHandler.handler(connects.get(address).getMetaData(), src, out);
                        break;
                    case 5:
                        String stmtId = readShortLen(src);
                        StatementHandler.handler(connects.get(address).getStatement(stmtId), src, out);
                        break;
                    case 6:
                        stmtId = readShortLen(src);
                        String rsId = readShortLen(src);
                        ResultSetHandler.handler(connects.get(address).getStatement(stmtId).getResultSet(rsId),
                                src, out);
                        break;
                    case 7:
                        stmtId = readShortLen(src);
                        PreparedStatementHandler.handler(connects.get(address).getPrepareStatement(stmtId),
                                src, out);
                    default:
                        logger.error("cmd[" + cmd + "] is not defined");
                }
            } catch (SQLException e) {
                logger.error(address, e);
                out.write(writeShortStr(ERROR, e.getMessage()));
            }
        }
        src.release();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(ctx.channel().remoteAddress() + " connect");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String address = ctx.channel().remoteAddress().toString();
        closeConn(address);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String address = ctx.channel().remoteAddress().toString();
        logger.error(address, cause);
        closeConn(address);
        ctx.close();
    }

    private void closeConn(String key) {
        WrapConnect conn = connects.remove(key);
        if (conn != null) conn.close();
    }

}