package com;

import com.handler.*;
import com.jdbc.bean.*;
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
    final ConcurrentMap<String, WrapConnect> connects = new ConcurrentHashMap<>();


    @Override
    public void channelRead(ChannelHandlerContext out, Object obj) {
        String address = out.channel().remoteAddress().toString();
        String connectId = md5(address);
        ByteBuf src = (ByteBuf) obj;
        boolean finish = false;
        while (!finish && src.isReadable()) {
            short cmd = src.readByte();
            try {
                switch (cmd) {
                    case ~0:
                        finish = true;
                        break;
                    case 2:
                        String dbKey = readShortLen(src);
                        String properties = readIntLen(src);
                        WrapConnect conn = new WrapConnect(connectId, dbKey, properties);
                        if (connects.containsKey(connectId)) closeConn(connectId);
                        connects.put(connectId, conn);
                        out.write(writeByte(OK));
                        break;
                    case 3:
                        WrapConnect wrapConnect = connects.get(connectId);
                        wrapConnect.updateTime(System.currentTimeMillis());
                        ConnectHandler.handler(wrapConnect, src, out);
                        break;
                    case 4:
                        wrapConnect = connects.get(connectId);
                        wrapConnect.updateTime(System.currentTimeMillis());
                        ConnectMetaHandler.handler(wrapConnect, src, out);
                        break;
                    case 5:
                        String stmtId = readShortLen(src);
                        WrapStatement wrapStatement = connects.get(connectId).getStatement(stmtId);
                        wrapStatement.updateTime();
                        StatementHandler.handler(wrapStatement, src, out);
                        break;
                    case 6:
                        stmtId = readShortLen(src);
                        wrapStatement = connects.get(connectId).getStatement(stmtId);
                        wrapStatement.updateTime();
                        String rsId = readShortLen(src);
                        ResultSetHandler.handler(wrapStatement.getResultSet(rsId), src, out);
                        break;
                    case 7:
                        stmtId = readShortLen(src);
                        WrapPrepareStatement wrapPrepareStatement = connects.get(connectId).getPrepareStatement(stmtId);
                        wrapPrepareStatement.updateTime();
                        PrepareStatementHandler.handler(wrapPrepareStatement, src, out);
                    case 8:
                        connects.get(connectId).updateTime(System.currentTimeMillis());
                        out.write(IOHandler.writeByte(OK));
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