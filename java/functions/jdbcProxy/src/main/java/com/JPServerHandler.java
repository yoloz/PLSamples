package com;

import com.handler.IOHandler;
import com.handler.JdbcHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

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
    private Connection conn;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf m = (ByteBuf) msg;
        while (m.isReadable()) {
            short cmd = m.readUnsignedByte();
            if (cmd == 1) {
                try {
                    conn = IOHandler.requestConnect(m);
                    IOHandler.connOkP(ctx);
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                    IOHandler.errorP(ctx, e.getMessage());
                }
            } else {
                int sqlength = m.readInt();
                String sql = new String(ByteBufUtil.getBytes(m, m.readerIndex(), sqlength), StandardCharsets.UTF_8);
                m.readerIndex(m.readerIndex() + sqlength);
                if (cmd == 2) {
                    try {
                        int u = JdbcHandler.update(conn, sql);
                        IOHandler.updateOkP(ctx, u);
                    } catch (SQLException e) {
                        logger.error(e.getMessage(), e);
                        IOHandler.errorP(ctx, e.getMessage());
                    }
                } else if (cmd == 3) {
                    ResultSet rs = null;
                    ByteBuf buf = Unpooled.buffer();
                    try {
                        rs = JdbcHandler.query(conn, sql);
                        buf.writeByte(0x00);
                        ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();
                        buf.writeShort(colCount);
                        ctx.write(buf);
                        IOHandler.rsMetaOkP(ctx, md);
                        IOHandler.rsRowOkP(ctx, rs);
                    } catch (SQLException e) {
                        logger.error(e.getMessage(), e);
                        IOHandler.errorP(ctx, e.getMessage());
                    } finally {
                        if (rs != null) try {
                            rs.close();
                        } catch (SQLException ignore) {
                        }
                    }
                } else IOHandler.errorP(ctx, "cmd[" + cmd + "] is not support");
            }
        }
        m.release();
        logger.info("channelRead ref count[" + m.refCnt() + "]");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        closeConn();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getMessage(), cause);
        closeConn();
        ctx.close();
    }

    /**
     * close jdbc connection
     */
    private void closeConn() {
        if (conn != null) try {
            conn.close();
        } catch (SQLException ignore) {
        }
    }
}