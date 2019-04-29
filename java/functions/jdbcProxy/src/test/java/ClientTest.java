import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        byte[] user = "user".getBytes(StandardCharsets.UTF_8);
        byte[] pwd = "pwd".getBytes(StandardCharsets.UTF_8);
        byte[] db = "dbname".getBytes(StandardCharsets.UTF_8);
        byte[] pro = "useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=CONVERT_TO_NULL"
                .getBytes(StandardCharsets.UTF_8);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put((byte) 0x01);
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
        else {
            int errorLength = in.readInt();
            System.out.println("connect failure: " + readString(in, errorLength));
        }
    }

    @Test
    public void testQuery() throws IOException {

        testConnect();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] sql = "SELECT * FROM test.new_table".getBytes(StandardCharsets.UTF_8);

        buffer.put((byte) 0x03);
        buffer.putInt(sql.length);
        buffer.put(sql);
        byte[] bytes = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, bytes, 0, buffer.position());

        out.write(bytes);
        out.flush();
        buffer.clear();

        short result = in.readByte();
        if (result == 0) {
            System.out.println("query success");
            short colCount = in.readShort();
            System.out.println("resultSetMeta array ========");
            for (int i = 0; i < colCount; i++) {
                short cl = in.readShort();
                System.out.println("catalogName: " + readString(in, cl));
                short scl = in.readShort();
                System.out.println("schemaName: " + readString(in, scl));
                short tl = in.readShort();
                System.out.println("tableName: " + readString(in, tl));
                short col = in.readShort();
                System.out.println("columnLabel: " + readString(in, col));
                short coln = in.readShort();
                System.out.println("columnName: " + readString(in, coln));
                short colt = in.readShort();
                System.out.println("columnTypeName: " + readString(in, colt));
                System.out.println("columnDisplaySize: " + in.readInt());
                System.out.println("precision: " + in.readInt());
                System.out.println("scale: " + in.readInt());
                System.out.println("columnType: " + in.readInt());
                System.out.println("resultSetMeta one column field ========");
            }
            System.out.println("resultSetRow========");
            while (true) {
                byte cmd = in.readByte();
                if (cmd == (byte) 0x7f) {
                    for (int i = 0; i < colCount; i++) {
                        int l = in.readInt();
                        if (l == ~0) System.out.println("value is null");
                            //todo 根据上述元数据类型转换，临时string输出
                        else System.out.println("value: " + readString(in, l));
                    }
                } else if (cmd == (byte) 0xff) break;
                else throw new IOException("cmd is not defined[" + cmd + "]");
            }
        } else {
            int errorLength = in.readInt();
            byte[] error = new byte[errorLength];
            in.readFully(error, 0, errorLength);
            System.out.println("query failure: " + new String(error, StandardCharsets.UTF_8));
        }

    }

    private String readString(InputStream in, int length) throws IOException {
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
