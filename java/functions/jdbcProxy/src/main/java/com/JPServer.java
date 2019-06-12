package com;

import com.util.Constants;
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

import java.util.concurrent.*;


public class JPServer {

    private final EventLoopGroup mainG = new NioEventLoopGroup(1);
    private final EventLoopGroup workerG = new NioEventLoopGroup();
    private final ScheduledExecutorService timeout = Executors.newSingleThreadScheduledExecutor();

    private JPServer() {
    }

    public static void main(String[] args) throws Exception {
        // Configure the server.
        final JPServer jpServer = new JPServer();
        final JPServerHandler serverHandler = new JPServerHandler();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(jpServer.mainG, jpServer.workerG)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(serverHandler);
                        }
                    });

            jpServer.timeout.scheduleWithFixedDelay(new Thread(() ->
                            serverHandler.connects.forEach((k, v) -> {
                                if (v.isTimeout()) serverHandler.connects.remove(k);
                            })),
                    Constants.proxyTimeout, Constants.proxyTimeout, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                serverHandler.connects.forEach((k, v) -> v.close());
                serverHandler.connects.clear();
                jpServer.timeout.shutdown();
                jpServer.mainG.shutdownGracefully();
                jpServer.workerG.shutdownGracefully();
            }));
            // Start the server.
            ChannelFuture f = b.bind(Constants.proxyPort).sync();
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            jpServer.mainG.shutdownGracefully();
            jpServer.workerG.shutdownGracefully();
        }
    }
}
