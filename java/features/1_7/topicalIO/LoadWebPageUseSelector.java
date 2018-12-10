package topicalIO;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * 如果程序对网络操作的并发性和吞吐量的要求比较高，比较好的方式是通过非阻塞式的套接字通道实现多路复用或者使用NIO.2中的异步套接字通道
 * 多路复用套接字通道
 *  on 16-7-10.
 * 通过一个专门的选择器(selector)来同时对多个套接字通道进行监听。当其中的某些套接字通道上有它感兴趣的事件发生时，这些通道会变为可用状态，
 * 可以在选择器的选择中被选中。
 */
public class LoadWebPageUseSelector {

    private void register(Selector selector, SocketAddress address) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(address);
        channel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
    }

    private Map<SocketAddress, String> urlToSocketAddress(Set<URL> urls) {
        Map<SocketAddress, String> map = new HashMap<>(urls.size());
        for (URL url : urls) {
            int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
            SocketAddress address = new InetSocketAddress(url.getHost(), port);
            String path = url.getFile();
            map.put(address, path);
        }
        return map;
    }


    public void load(Set<URL> urls) throws IOException {
        Map<SocketAddress, String> mapping = this.urlToSocketAddress(urls);
        Selector selector = Selector.open();
        for (SocketAddress address : mapping.keySet()) {
            register(selector, address);
        }
        int finished = 0, total = mapping.size();
        ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
        int len;
        while (finished < total) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                if (selectionKey.isValid() && selectionKey.isReadable()) {
                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                    String fileName = ((InetSocketAddress) channel.getRemoteAddress()).getHostName() + ".txt";
                    FileChannel fileChannel = FileChannel.open(Paths.get("/XXX/jade", fileName), StandardOpenOption.APPEND,
                            StandardOpenOption.CREATE);
                    buffer.clear();
                    while ((len = channel.read(buffer)) > 0 || buffer.position() != 0) {
                        buffer.flip();
                        fileChannel.write(buffer);
                        buffer.compact();
                    }
                    if (len == -1) {
                        finished++;
                        selectionKey.cancel();
                    }
                } else if (selectionKey.isValid() && selectionKey.isConnectable()) {
                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                    /**
                     * 以连接完成的事件来说，这次连接可能成功也可能失败。通过finishConnect方法可以完成连接，同时判断连接是否成功建立；
                     */
                    boolean success = channel.finishConnect();
                    if (!success) { //连接建立失败
                        /**If this channel is in non-blocking mode then this method will return <tt>false</tt>
                         if the connection process is not yet complete.
                         这里也可以不处理，再次select的时候可能连接就可以了**/
                        System.out.println("地址 " + channel.getRemoteAddress() + "连接出错，关闭连接.....");
                        finished++;
                        selectionKey.cancel();  //取消选择器对此通道的管理
                    } else {
                        InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
                        String request = "GET " + mapping.get(address) + " HTTP/1.0\r\n\r\nHost: "
                                + address.getHostString() + "\r\n\r\n";
                        System.out.println("请求数据头 " + request);
                        ByteBuffer header = ByteBuffer.wrap(request.getBytes("UTF-8"));
                        channel.write(header);
                    }
                }
            }
        }
    }

    /**
     * getHostName()：Gets the hostname. Note: This method may trigger a name service reverse lookup if the address was created with a literal IP address.
     * getHostString()：Returns the hostname, or the String form of the address if it doesn't have a hostname (it was created using a literal). This has the benefit of not attempting a reverse lookup.
     * 建议用hostString
     */
    public void differHostNameAndHostString() {
        InetSocketAddress address = new InetSocketAddress("http://www.baidu.com", 80);
        System.out.println(address.getHostName());
        System.out.println(address.getAddress());
        System.out.println(address.getHostString());
    }

    public static void main(String[] args) {
        LoadWebPageUseSelector selector = new LoadWebPageUseSelector();
        try {
//            selector.differHostNameAndHostString();
            Set<URL> urls = new HashSet<>(3);
            urls.add(new URL("http://www.baidu.com/"));
            urls.add(new URL("http://www.zongheng.com"));
            urls.add(new URL("http://www.163.com/"));
            selector.load(urls);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
