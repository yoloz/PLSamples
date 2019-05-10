package com;

import com.auth.SqlAuth;
import com.handler.IOHandler;
import com.handler.JdbcHandler;
import com.source.Connect;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 连接: cmd+categoryLength+category+usrLength+username+pwdLength+pwd
 * <p>
 * cmd+sqlLength+sql
 * <p>
 * cmd:
 * +-------------------+------------------+
 * |       cmd         |        desc      |
 * +-------------------+------------------+
 * |       0x01       |    connect      |
 * |       0x02       | update[create,update,delete]  |
 * |       0x03       |    query[select]      |
 * <p>
 */
@ChannelHandler.Sharable
public class JPServerHandler extends ChannelInboundHandlerAdapter {

    private final Logger logger = Logger.getLogger(JPServerHandler.class);
    private final ConcurrentMap<String, Connect> connects = new ConcurrentHashMap<>();


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String addr = ctx.channel().remoteAddress().toString();
        ByteBuf m = (ByteBuf) msg;
        while (m.isReadable()) {
            short cmd = m.readUnsignedByte();
            if (cmd == 1) {
                try {
                    Connect conn = IOHandler.requestConnect(m);
                    if (connects.containsKey(addr)) closeConn(addr);
                    connects.put(addr, conn);
                    IOHandler.connOkP(ctx);
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                    IOHandler.errorP(ctx, e.getMessage());
                }
            } else {
                int sqlength = m.readInt();
                String sql = new String(ByteBufUtil.getBytes(m, m.readerIndex(), sqlength), StandardCharsets.UTF_8);
                m.readerIndex(m.readerIndex() + sqlength);
                SqlAuth sqlAuth = new SqlAuth(connects.get(addr), sql);
                try {
                    if (sqlAuth.check()) {
                        if (cmd == 2) {
                            int u = JdbcHandler.update(connects.get(addr).getConnection(), sql);
                            IOHandler.updateOkP(ctx, u);
                        } else if (cmd == 3) {
                            ByteBuf buf = Unpooled.buffer();
                            ResultSet rs = JdbcHandler.query(connects.get(addr).getConnection(), sql);
                            buf.writeByte(0x00);
                            ResultSetMetaData md = rs.getMetaData();
                            int colCount = md.getColumnCount();
                            buf.writeShort(colCount);
                            ctx.write(buf);
                            IOHandler.rsMetaOkP(ctx, md);
                            IOHandler.rsRowOkP(ctx, rs);
                            try {
                                rs.close();
                            } catch (SQLException ignored) {
                            }
                        } else IOHandler.errorP(ctx, "cmd[" + cmd + "] is not support");
                    } else IOHandler.errorP(ctx, "no permission to execute sql[" + sql + "]");
                } catch (JSQLParserException | SQLException e) {
                    logger.error(e.getMessage(), e);
                    IOHandler.errorP(ctx, e.getMessage());
                }
            }
        }
        m.release();
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
        closeConn(ctx.channel().remoteAddress().toString());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getMessage(), cause);
        closeConn(ctx.channel().remoteAddress().toString());
        ctx.close();
    }

    private void closeConn(String key) {
        Connect conn = connects.remove(key);
        if (conn != null) conn.close();
    }

}