package topicalIO;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.charset.Charset;

/**
 *  on 16-8-21.
 * 进行组播消息的客户端实现
 */
public class TimeClient {

    public void start() throws IOException {
        NetworkInterface ni = NetworkInterface.getByName("wlan0");
        int port = 5000;
        try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni)) {
            InetAddress group = InetAddress.getByName("224.0.0.2");
            MembershipKey key = dc.join(group, ni);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            dc.receive(buffer);
            buffer.flip();
            byte[] data = new byte[buffer.limit()];
            buffer.get(data);
            String str = new String(data, Charset.forName("UTF-8"));
            System.out.println(str);
            key.drop();
        }
    }

    public static void main(String[] args) {
        TimeClient timeClient = new TimeClient();
        try {
            timeClient.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
