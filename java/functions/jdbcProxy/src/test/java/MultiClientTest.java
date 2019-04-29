import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MultiClientTest {

    public static void main(String[] args) throws IOException {
        new ClientImpl("mysql2", "xxxx", "xxxxx", "xxxxx",
                "useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=CONVERT_TO_NULL",
                "SELECT * FROM lgjob").start();

        new ClientImpl("mysql2", "xxxx", "xxxxx", "xxxxxx",
                "useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=CONVERT_TO_NULL",
                "SELECT * FROM ciisource").start();

    }


    private static class ClientImpl extends Thread {

        private String keyWord;
        private String userName;
        private String password;
        private String dbName;
        private String properties;
        private String stringSql;

        private Socket socket;

        public ClientImpl(String keyWord, String userName, String pwd, String dbName, String properties, String sql)
                throws IOException {
            this.keyWord = keyWord;
            this.userName = userName;
            this.password = pwd;
            this.dbName = dbName;
            this.properties = properties;
            this.stringSql = sql;

            this.socket = new Socket("127.0.0.1", 8007);
        }

        @Override
        public void run() {
            Logger logger = Logger.getLogger(this.getName());
            byte[] keyword = keyWord.getBytes(StandardCharsets.UTF_8);
            byte[] user = userName.getBytes(StandardCharsets.UTF_8);
            byte[] pwd = password.getBytes(StandardCharsets.UTF_8);
            byte[] db = dbName.getBytes(StandardCharsets.UTF_8);
            byte[] pro = properties.getBytes(StandardCharsets.UTF_8);
            try {
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
                if (result == 0) {
                    byte[] sql = stringSql.getBytes(StandardCharsets.UTF_8);

                    buffer.put((byte) 0x03);
                    buffer.putInt(sql.length);
                    buffer.put(sql);
                    bytes = new byte[buffer.position()];
                    System.arraycopy(buffer.array(), 0, bytes, 0, buffer.position());

                    out.write(bytes);
                    out.flush();
                    buffer.clear();

                    result = in.readByte();
                    if (result == 0) {
                        logger.info("query success");
                        short colCount = in.readShort();
                        logger.info("resultSetMeta array ========");
                        for (int i = 0; i < colCount; i++) {
                            short cl = in.readShort();
                            logger.info("catalogName: " + readString(in, cl));
                            short scl = in.readShort();
                            logger.info("schemaName: " + readString(in, scl));
                            short tl = in.readShort();
                            logger.info("tableName: " + readString(in, tl));
                            short col = in.readShort();
                            logger.info("columnLabel: " + readString(in, col));
                            short coln = in.readShort();
                            logger.info("columnName: " + readString(in, coln));
                            short colt = in.readShort();
                            logger.info("columnTypeName: " + readString(in, colt));
                            logger.info("columnDisplaySize: " + in.readInt());
                            logger.info("precision: " + in.readInt());
                            logger.info("scale: " + in.readInt());
                            logger.info("columnType: " + in.readInt());
                            logger.info("resultSetMeta one column field ========");
                        }
                        logger.info("resultSetRow========");
                        while (true) {
                            byte cmd = in.readByte();
                            if (cmd == (byte) 0x7f) {
                                for (int i = 0; i < colCount; i++) {
                                    int l = in.readInt();
                                    if (l == ~0) logger.info("value is null");
                                        //todo 根据上述元数据类型转换，临时string输出
                                    else logger.info("value: " + readString(in, l));
                                }
                            } else if (cmd == (byte) 0xff) break;
                            else throw new IOException("cmd is not defined[" + cmd + "]");
                        }
                    } else {
                        int errorLength = in.readInt();
                        byte[] error = new byte[errorLength];
                        in.readFully(error, 0, errorLength);
                        logger.info("query failure: " + new String(error, StandardCharsets.UTF_8));
                    }
                } else {
                    int errorLength = in.readInt();
                    logger.info("connect failure: " + readString(in, errorLength));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String readString(InputStream in, int length) throws IOException {
            byte[] bytes = new byte[length];
            in.read(bytes, 0, length);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
