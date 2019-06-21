package com.handler;

import com.audit.AuditEvent;
import com.audit.AuditManager;
import com.jdbc.bean.*;
import com.jdbc.sql.parser.SQLParserUtils;
import com.jdbc.sql.parser.SQLStatementParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static com.handler.IOHandler.*;

public class StatementHandler {

    public static void handler(WrapStatement statement, ByteBuf src, ChannelHandlerContext out)
            throws SQLException, PermissionException {
        String mName = IOHandler.readByteLen(src);
        if ("executeQuery".equals(mName)) {
            String sql = readIntLen(src);
            AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                    statement.getUser(), mName, sql));
            UserHandler.authSql(statement.getWrapConnect(), statement.getUser(), sql);
            statement.executeQuery(sql, out);
        } else if ("executeUpdate".equals(mName)) {
            WrapConnect connect = statement.getWrapConnect();
            short pc = src.readByte();
            String sql = readIntLen(src);
            SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, connect.getDbTypeLower());
            parser.setDefaultDbName(connect.getDefaultDb());
            parser.setConn(connect);
            List<SqlInfo> list = parser.parseToSQLInfo();
            UserHandler.authSql(statement.getUser(), connect.getAK(), list);
            sql = parser.encryptStmtSql(statement.getUser());
            int count;
            if (1 == pc) {
                AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                        statement.getUser(), mName, sql));
                count = statement.executeUpdate(sql);
            } else {
                int arrSize = src.readShort();
                short type = src.readByte();
                if (0 == arrSize) {
                    if (0 != type) throw new SQLException("executeUpdate[autoGeneratedKeys] type[" +
                            type + "] error");
                    int v = src.readInt();
                    AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                            statement.getUser(), mName, sql, v));
                    count = statement.executeUpdate(sql, v);
                } else {
                    if (0 == type) {
                        int[] columnIndexes = readInt(arrSize, src);
                        AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                                statement.getUser(), mName, sql, Arrays.toString(columnIndexes)));
                        count = statement.executeUpdate(sql, columnIndexes);
                    } else if (1 == type) {
                        String[] columnNames = readShortLen(arrSize, src);
                        AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                                statement.getUser(), mName, sql, Arrays.toString(columnNames)));
                        count = statement.executeUpdate(sql, columnNames);
                    } else throw new SQLException("executeUpdate[array] type[" + type + "] error");
                }
            }
            out.write(writeInt(OK, count));
        } else if ("execute".equals(mName)) {
            short pc = src.readByte();
            String sql = readIntLen(src);
            UserHandler.authSql(statement.getWrapConnect(), statement.getUser(), sql);
            boolean bool;
            if (1 == pc) {
                AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                        statement.getUser(), mName, sql));
                bool = statement.execute(sql);
            } else {
                int arrSize = src.readShort();
                short type = src.readByte();
                if (0 == arrSize) {
                    if (0 != type) throw new SQLException("execute[autoGeneratedKeys] type[" + type + "] error");
                    int v = src.readInt();
                    AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                            statement.getUser(), mName, sql, v));
                    bool = statement.execute(sql, v);
                } else {
                    if (0 == type) {
                        int[] columnIndexes = readInt(arrSize, src);
                        AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                                statement.getUser(), mName, sql, Arrays.toString(columnIndexes)));
                        bool = statement.execute(sql, columnIndexes);
                    } else if (1 == type) {
                        String[] columnNames = readShortLen(arrSize, src);
                        AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                                statement.getUser(), mName, sql, Arrays.toString(columnNames)));
                        bool = statement.execute(sql, columnNames);
                    } else throw new SQLException("executeUpdate[array] type[" + type + "] error");
                }
            }
            out.write(writeShortStr(OK, bool));
        } else if ("getGeneratedKeys".equals(mName)) {
            AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                    statement.getUser(), mName));
            statement.getGeneratedKeys(out);
        } else if ("executeBatch".equals(mName)) {
            int arrSize = src.readShort();
            short type = src.readByte();
            if (1 != type) throw new SQLException("executeBatch param type[" + type + "] error");
            String[] sqls = readIntLen(arrSize, src);
            AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                    statement.getUser(), mName, Arrays.toString(sqls)));
            for (String sql : sqls) UserHandler.authSql(statement.getWrapConnect(), statement.getUser(), sql);
            int[] code = statement.executeBatch(sqls);
            out.write(writeInt(OK, code));
        } else if ("setFetchDirection".equals(mName)) {
            int v = src.readInt();
            AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                    statement.getUser(), mName, v));
            statement.setFetchDirection(v);
            out.write(writeByte(OK));
        } else if ("setFetchSize".equals(mName)) {
            int v = src.readInt();
            AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                    statement.getUser(), mName, v));
            statement.setFetchSize(v);
            out.write(writeByte(OK));
        } else if ("getResultSet".equals(mName)) {
            AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                    statement.getUser(), mName));
            statement.getResultSet(out);
        } else if ("close".equals(mName)) {
            AuditManager.getInstance().audit(new AuditEvent(statement.getWrapConnect().getAddress(),
                    statement.getUser(), mName));
            statement.close();
            out.write(writeByte(OK));
        } else throw new SQLException("statementMethod[" + mName + "] is not support");
    }
}
