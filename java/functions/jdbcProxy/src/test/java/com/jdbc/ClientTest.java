package com.jdbc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ClientTest {

    Socket socket = null;

    @Before
    public void setUp() throws Exception {
        socket = new Socket("127.0.0.1", 8007);
    }

    @After
    public void tearDown() throws Exception {

        if (socket != null) socket.close();
    }

    /**
     * jdbc:mysql2://host:port/dbname?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=CONVERT_TO_NULL
     */
    @Test
    public void testConnect() throws IOException {
        byte[] keyword = "mysql2".getBytes(StandardCharsets.UTF_8);
        byte[] user = "lsjcj".getBytes(StandardCharsets.UTF_8);
        byte[] pwd = "ciilsjcj".getBytes(StandardCharsets.UTF_8);
        byte[] db = "fea_flow".getBytes(StandardCharsets.UTF_8);
        byte[] pro = "useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=CONVERT_TO_NULL"
                .getBytes(StandardCharsets.UTF_8);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 0x02);
        buffer.putShort((short) keyword.length);
        buffer.put(keyword);
        buffer.putShort((short) user.length);
        buffer.put(user);
        buffer.putShort((short) pwd.length);
        buffer.put(pwd);
        buffer.putShort((short) db.length);
        buffer.put(db);
        buffer.putInt(pro.length);
        buffer.put(pro);
        byte[] bytes = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, bytes, 0, buffer.position());
        out.write(bytes);
        out.flush();
        buffer.clear();
        short result = in.readByte();
        if (result == 0) System.out.println("connect success");
        else System.out.println("create connect failure: " + readShortString(in));
    }


    @Test
    public void testQuery() throws IOException {

        testConnect();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        ByteBuffer tempBuffer = ByteBuffer.allocate(1024);
        byte[] methodName, outBytes;
        short result;

        methodName = "setUser".getBytes(StandardCharsets.UTF_8);
        tempBuffer.put((byte) 0x03);
        tempBuffer.put((byte) methodName.length);
        tempBuffer.put(methodName);
        byte[] user = "test".getBytes(StandardCharsets.UTF_8);
        tempBuffer.put((byte) user.length);
        tempBuffer.put(user);
        outBytes = new byte[tempBuffer.position()];
        System.arraycopy(tempBuffer.array(), 0, outBytes, 0, tempBuffer.position());
        out.write(outBytes);
        out.flush();
        tempBuffer.clear();
        result = in.readByte();
        if (0 == result) System.out.println("setUser[test] success");
        else System.out.println("setUser[test] failure: " + readShortString(in));

        methodName = "createStatement".getBytes(StandardCharsets.UTF_8);
        tempBuffer.put((byte) 0x03);
        tempBuffer.put((byte) methodName.length);
        tempBuffer.put(methodName);
        tempBuffer.put((byte) 0);
        outBytes = new byte[tempBuffer.position()];
        System.arraycopy(tempBuffer.array(), 0, outBytes, 0, tempBuffer.position());
        out.write(outBytes);
        out.flush();
        tempBuffer.clear();
        result = in.readByte();
        String stmtId = null;
        if (result == 0) {
            stmtId = readShortString(in);
            System.out.println("createStatement success,stmt[" + stmtId + "]");
        } else System.out.println("create statement failure: " + readShortString(in));

        if (stmtId != null) {
            byte[] stmt = stmtId.getBytes(StandardCharsets.UTF_8);
            methodName = "executeQuery".getBytes(StandardCharsets.UTF_8);
            byte[] sql = "select * from lgservice".getBytes(StandardCharsets.UTF_8);
            tempBuffer.put((byte) 0x05);
            tempBuffer.putShort((short) stmt.length);
            tempBuffer.put(stmt);
            tempBuffer.put((byte) methodName.length);
            tempBuffer.put(methodName);
            tempBuffer.putInt(sql.length);
            tempBuffer.put(sql);
            outBytes = new byte[tempBuffer.position()];
            System.arraycopy(tempBuffer.array(), 0, outBytes, 0, tempBuffer.position());
            out.write(outBytes);
            out.flush();
            tempBuffer.clear();
            result = in.readByte();
            if (result == 0) {
                System.out.println("executeQuery success");
                System.out.println("resultSet: " + readShortString(in));
                System.out.println("**********RSMeta**********");
                short colCount = in.readShort();
                for (int i = 0; i < colCount; i++) {
                    System.out.print("catalogName:[" + readShortString(in));
                    System.out.print("],schemaName:[" + readShortString(in));
                    System.out.print("],tableName:[" + readShortString(in));
                    System.out.print("],columnLabel:[" + readShortString(in));
                    System.out.print("],columnName:[" + readShortString(in));
                    System.out.print("],columnTypeName:[" + readShortString(in));
                    System.out.print("],columnDisplaySize:[" + in.readInt());
                    System.out.print("],precision:[" + in.readInt());
                    System.out.print("],scale:[" + in.readInt());
                    System.out.print("],columnType:[" + in.readInt());
                    System.out.print("]\n");
                }
                System.out.println("**********RSMeta**********");
                System.out.println("**********RSRow**********");
                while (true) {
                    byte cmd = in.readByte();
                    if (cmd == (byte) 0x7e) {
                        for (int i = 0; i < colCount; i++) {
                            System.out.print("val:[" + readIntString(in));
                            if (i != colCount - 1) System.out.print("],");
                            else System.out.print("]");
                        }
                        System.out.println();
                    } else if (cmd == (byte) 0x7f) break;
                    else throw new IOException("cmd is not defined[" + cmd + "]");
                }
                System.out.println("**********RSRow**********");
            } else System.out.println("executeQuery failure: " + readShortString(in));
        }
    }

    @Test
    public void testExecute() throws IOException {

        testConnect();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        byte[] methodName = "createStatement".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 0x03);
        buffer.put((byte) methodName.length);
        buffer.put(methodName);
        buffer.put((byte) 0);
        byte[] bytes = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, bytes, 0, buffer.position());
        out.write(bytes);
        out.flush();
        buffer.clear();
        short result = in.readByte();
        String stmtId = null;
        if (result == 0) {
            stmtId = readShortString(in);
            System.out.println("createStatement success,stmt[" + stmtId + "]");
        } else System.out.println("create statement failure: " + readShortString(in));

        if (stmtId != null) {
            execute(stmtId,
                    "create table test (name varchar(200))".getBytes(StandardCharsets.UTF_8),
                    buffer, in, out);
            execute(stmtId,
                    "insert into test values ('test')".getBytes(StandardCharsets.UTF_8),
                    buffer, in, out);
            execute(stmtId,
                    "insert into test values ('test2')".getBytes(StandardCharsets.UTF_8),
                    buffer, in, out);
            execute(stmtId,
                    "insert into test values ('test3')".getBytes(StandardCharsets.UTF_8),
                    buffer, in, out);
        }
    }

    private void execute(String stmtId, byte[] sql,
                         ByteBuffer buffer, DataInputStream in, DataOutputStream out)
            throws IOException {
        byte[] stmt = stmtId.getBytes(StandardCharsets.UTF_8);
        byte[] methodName = "execute".getBytes(StandardCharsets.UTF_8);
        buffer.put((byte) 0x05);
        buffer.putShort((short) stmt.length);
        buffer.put(stmt);
        buffer.put((byte) methodName.length);
        buffer.put(methodName);
        buffer.put((byte) 0x01);
        buffer.putInt(sql.length);
        buffer.put(sql);
        byte[] bytes = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, bytes, 0, buffer.position());
        out.write(bytes);
        out.flush();
        buffer.clear();
        short result = in.readByte();
        if (result == 0) {
            System.out.println("execute success");
            System.out.println("execute result: " + readShortString(in));
        } else System.out.println("execute failure: " + readShortString(in));
    }


    private String readShortString(DataInputStream in) throws IOException {
        short length = in.readShort();
        if (length == ~0) return null;
        byte[] bytes = new byte[length];
        in.read(bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String readIntString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length == ~0) return null;
        byte[] bytes = new byte[length];
        in.read(bytes, 0, length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test
    public void convertInt2Short() {
        int i = 200;
        short s = int2Short(i);
        assertEquals(s, (short) 200);
    }

    public static byte[] int2Bytes(int a) {

        byte[] b = new byte[4];
        b[0] = (byte) (a >> 24);
        b[1] = (byte) (a >> 16);
        b[2] = (byte) (a >> 8);
        b[3] = (byte) (a);

        return b;
    }

    public static int bytes2Int(byte[] b) {
        return ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16)
                | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
    }

    public static short int2Short(int i) {
        byte[] b = int2Bytes(i);
        return (short) (((b[2] & 0xff) << 8) | (b[3] & 0xff));
    }
}
