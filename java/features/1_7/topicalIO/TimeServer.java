package topicalIO;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Date;

/**
 *  on 16-8-21.
 * 进行组播的服务器端实现
 */
public class TimeServer {

    public void start() throws IOException {
        try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET).bind(null)) {
            System.out.println(dc.getLocalAddress());
            InetAddress group = InetAddress.getByName("224.0.0.2");
            int port = 5000;
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
                String str = (new Date()).toString();
                dc.send(ByteBuffer.wrap(str.getBytes(Charset.forName("UTF-8"))), new InetSocketAddress(group, port));
            }
        }
    }

    public static void main(String[] args) {
        TimeServer timeServer = new TimeServer();
        try {
            timeServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
