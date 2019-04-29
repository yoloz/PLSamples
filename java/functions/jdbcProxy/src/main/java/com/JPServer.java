package com;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JPServer {

    public static final String JPPath = System.getProperty("JPPath");

    private final Properties properties = new Properties();

    private final EventLoopGroup mainG = new NioEventLoopGroup(1);
    private final EventLoopGroup workerG = new NioEventLoopGroup();

    private JPServer() {
        try (InputStream in = getClass().getResourceAsStream("/conf.properties")) {
            properties.load(in);
        } catch (IOException e) {
            System.err.println("init conf error[" + e + "]");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        // Configure the server.
        final JPServer jpServer = new JPServer();
        final JPServerHandler serverHandler = new JPServerHandler();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(jpServer.mainG, jpServer.workerG)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(serverHandler);
                        }
                    });
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                jpServer.mainG.shutdownGracefully();
                jpServer.workerG.shutdownGracefully();
            }));
            // Start the server.
            ChannelFuture f = b.bind(
                    Integer.parseInt(jpServer.properties.getProperty("port", "8007")))
                    .sync();
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            jpServer.mainG.shutdownGracefully();
            jpServer.workerG.shutdownGracefully();
        }
    }
}
