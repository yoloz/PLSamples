package topicalIO;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *  on 16-6-26.
 */
public class ChannelTest {

    /**
     * 打开文件通道并写入数据
     */
    public void openAndWrite() throws IOException {
//        Path file = Paths.get("my.txt");
//        System.out.println(file.toUri().getPath());
        FileChannel fileChannel = FileChannel.open(Paths.get("my.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        byteBuffer.putChar('A').flip();
        fileChannel.write(byteBuffer);
        fileChannel.close();
    }

    /**
     * 对文件通道的绝对位置进行读写操作的示例
     */
    public void readWriteAbsolute() throws IOException {
        FileChannel fileChannel = FileChannel.open(Paths.get("absolute.txt"), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        ByteBuffer writeBuffer = ByteBuffer.allocate(4).putChar('A').putChar('B');
        writeBuffer.flip();
        fileChannel.write(writeBuffer, 1024);
        ByteBuffer readBuffer = ByteBuffer.allocate(2);
        fileChannel.read(readBuffer, 1026);
        readBuffer.flip();
        System.out.println(readBuffer.getChar());
        fileChannel.close();
    }

    /**
     * 使用文件通道保存网页内容
     *
     * @throws IOException
     */
    public void loadWebPage(String url) throws IOException {
        FileChannel channel = FileChannel.open(Paths.get("content.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        InputStream inputStream = new URL(url).openStream();
        ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
        channel.transferFrom(readableByteChannel, 0, Integer.MAX_VALUE);
        readableByteChannel.close();
        channel.close();
    }

    /**
     * 用字节缓冲区进行文件复制
     */
    public void copyUseByteBuffer() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(32 * 1024);
        try (FileChannel src = FileChannel.open(Paths.get("src.txt"), StandardOpenOption.READ);
             FileChannel dest = FileChannel.open(Paths.get("dest.txt"), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            while (src.read(byteBuffer) > 0 || byteBuffer.position() != 0) {
                byteBuffer.flip();
                dest.write(byteBuffer);
                byteBuffer.compact();
            }
        }
    }

    /**
     * 使用文件通道进行文件复制
     */
    public void copyUseChanelTransfer() throws IOException {
        try (FileChannel src = FileChannel.open(Paths.get("src.txt"), StandardOpenOption.READ);
             FileChannel dest = FileChannel.open(Paths.get("dest.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            src.transferTo(0, src.size(), dest);
        }
    }

    /**
     * 内存映射文件
     */
    public void mapFile() throws IOException {
        try (FileChannel channel = FileChannel.open(Paths.get("src.data"), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
            byte b = buffer.get(1024 * 1024);
            buffer.put(5 * 1024 * 1024, b);
            buffer.force();
        }
    }

    /**
     * 阻塞式客户端套接字的使用
     */
    public void loadWebPageUseSocket() throws IOException {
        try (FileChannel channel = FileChannel.open(Paths.get("webSocket.txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("www.baidu.com", 80))) {
            String request = "GET / HTTP/1.1\r\n\r\nHost: www.baidu.com\r\n\r\n";
            ByteBuffer header = ByteBuffer.wrap(request.getBytes("UTF-8"));
            socketChannel.write(header);
            channel.transferFrom(socketChannel, 0, Integer.MAX_VALUE);
        }
    }

    /**
     * 阻塞式服务器端套接字使用
     *
     * @throws IOException
     */
    public void startSimpleServer() throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress("localhost", 10800));
        while (true) {
            try (SocketChannel socket = channel.accept()) {
                socket.write(ByteBuffer.wrap("Hello".getBytes("UTF-8")));
            }
        }

    }


    public static void main(String[] args) {
        ChannelTest channelTest = new ChannelTest();
        try {
//            channelTest.openAndWrite();
//            channelTest.loadWebPage("http://www.baidu.com");
//            channelTest.loadWebPageUseSocket();
            channelTest.startSimpleServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
